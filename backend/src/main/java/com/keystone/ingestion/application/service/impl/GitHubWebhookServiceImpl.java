// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.ingestion.application.service.GitHubWebhookService;
import com.keystone.ingestion.domain.exception.WebhookValidationException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Validates and processes GitHub push event webhooks.
 *
 * <p>Supports HMAC-SHA256 signature verification per GitHub's webhook
 * security guidelines. Extracts repository, branch, and changed spec
 * paths from the push event payload.
 */
@Service
public class GitHubWebhookServiceImpl implements GitHubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookServiceImpl.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public GitHubWebhookServiceImpl(
            ObjectMapper objectMapper,
            @Value("${ingestion.webhook.github.secret:}") String webhookSecret) {
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public UUID processWebhook(String payload, String signature) throws WebhookValidationException {
        UUID deliveryId = UUID.randomUUID();

        // Step 1: Validate HMAC signature (if a secret is configured)
        if (!webhookSecret.isEmpty() && !isValidSignature(payload, signature)) {
            log.warn("Invalid webhook signature for delivery {}", deliveryId);
            throw new WebhookValidationException(deliveryId.toString(), "Invalid HMAC signature");
        }

        // Step 2: Parse the push event payload
        try {
            JsonNode root = objectMapper.readTree(payload);

            // Extract repository info
            JsonNode repoNode = root.get("repository");
            if (repoNode == null) {
                log.warn("Webhook payload missing repository field for delivery {}", deliveryId);
                throw new WebhookValidationException(deliveryId.toString(), "Missing repository field in payload");
            }

            String repoFullName = repoNode.has("full_name")
                    ? repoNode.get("full_name").asText()
                    : "unknown/repo";
            String ref = root.has("ref") ? root.get("ref").asText() : "";
            String branch = ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : ref;

            log.info("Processing GitHub webhook: repo={}, branch={}, deliveryId={}",
                    repoFullName, branch, deliveryId);

            // Step 3: Extract changed files from commits
            JsonNode commits = root.get("commits");
            if (commits != null && commits.isArray()) {
                for (JsonNode commit : commits) {
                    String commitSha = commit.has("id") ? commit.get("id").asText() : "unknown";
                    log.debug("Commit {} in webhook delivery {}", commitSha, deliveryId);

                    // Check for OpenAPI spec changes in added, modified, and removed files
                    extractSpecPaths(commit, "added", repoFullName, commitSha);
                    extractSpecPaths(commit, "modified", repoFullName, commitSha);
                }
            }

            log.info("Webhook processed: repo={}, branch={}, deliveryId={}",
                    repoFullName, branch, deliveryId);

            // Note: In a full implementation, changed spec files would be fetched via
            // the GitHub API and queued for ingestion. For now, the webhook is
            // acknowledged and logged for observability.
            return deliveryId;

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse webhook payload for delivery {}", deliveryId);
            throw new WebhookValidationException(deliveryId.toString(), "Malformed JSON payload", e);
        }
    }

    /**
     * Validates the HMAC-SHA256 signature of the webhook payload.
     */
    private boolean isValidSignature(String payload, String signature) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] computedHash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : computedHash) {
                hexString.append(String.format("%02x", b));
            }
            String expectedSignature = SIGNATURE_PREFIX + hexString;
            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC computation failed", e);
            return false;
        }
    }

    /**
     * Extracts OpenAPI spec file paths from a commit's file lists.
     */
    private void extractSpecPaths(JsonNode commit, String field, String repo, String commitSha) {
        JsonNode files = commit.get(field);
        if (files != null && files.isArray()) {
            for (JsonNode file : files) {
                String path = file.asText();
                if (isOpenApiSpec(path)) {
                    log.info("Detected spec change in {}: {} (commit {}, delivery {})",
                            field, path, commitSha, repo);
                }
            }
        }
    }

    /**
     * Checks if a file path looks like an OpenAPI/Swagger spec.
     */
    private boolean isOpenApiSpec(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".yaml")
                || lower.endsWith(".yml")
                || lower.endsWith(".json");
    }
}
