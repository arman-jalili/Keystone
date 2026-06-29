// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
package com.keystone.ingestion.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.ingestion.application.dto.IncomingSpec;
import com.keystone.ingestion.application.service.GitHubWebhookService;
import com.keystone.ingestion.application.service.IngestionService;
import com.keystone.ingestion.domain.exception.WebhookValidationException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
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
 * <p>Verifies HMAC-SHA256 signatures, extracts changed spec paths from
 * push events, fetches the actual spec content from the GitHub API,
 * and pushes the content into the ingestion pipeline.
 */
@Service
public class GitHubWebhookServiceImpl implements GitHubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookServiceImpl.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final ObjectMapper objectMapper;
    private final String webhookSecret;
    private final String githubToken;
    private final IngestionService ingestionService;
    private final HttpClient httpClient;

    public GitHubWebhookServiceImpl(
            ObjectMapper objectMapper,
            @Value("${ingestion.webhook.github.secret:}") String webhookSecret,
            @Value("${ingestion.webhook.github.token:}") String githubToken,
            IngestionService ingestionService) {
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
        this.githubToken = githubToken;
        this.ingestionService = ingestionService;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public UUID processWebhook(String payload, String signature) throws WebhookValidationException {
        UUID deliveryId = UUID.randomUUID();

        // Step 1: Validate HMAC signature
        if (!webhookSecret.isEmpty() && !isValidSignature(payload, signature)) {
            log.warn("Invalid webhook signature for delivery {}", deliveryId);
            throw new WebhookValidationException(deliveryId.toString(), "Invalid HMAC signature");
        }

        // Step 2: Parse the push event payload
        try {
            JsonNode root = objectMapper.readTree(payload);

            JsonNode repoNode = root.get("repository");
            if (repoNode == null) {
                throw new WebhookValidationException(deliveryId.toString(), "Missing repository field in payload");
            }

            String repoFullName =
                    repoNode.has("full_name") ? repoNode.get("full_name").asText() : "unknown/repo";
            String defaultBranch = repoNode.has("default_branch")
                    ? repoNode.get("default_branch").asText()
                    : "main";
            String ref = root.has("ref") ? root.get("ref").asText() : "";
            String branch = ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : ref;
            if (branch.isEmpty()) branch = defaultBranch;

            log.info("Processing GitHub webhook: repo={}, branch={}, deliveryId={}", repoFullName, branch, deliveryId);

            // Step 3: Collect changed spec paths from commits
            List<SpecChange> changedSpecs = new ArrayList<>();
            JsonNode commits = root.get("commits");
            if (commits != null && commits.isArray()) {
                for (JsonNode commit : commits) {
                    String commitSha = commit.has("id") ? commit.get("id").asText() : "unknown";
                    extractSpecPaths(commit, "added", repoFullName, commitSha, changedSpecs);
                    extractSpecPaths(commit, "modified", repoFullName, commitSha, changedSpecs);
                }
            }

            // Step 4: If no commits (e.g. delete event), fall back to the head commit
            if (changedSpecs.isEmpty()) {
                String headSha = root.has("after") ? root.get("after").asText() : null;
                if (headSha != null && !headSha.matches("0+")) {
                    log.info("No spec changes in commits, checking head commit {} for {}", headSha, repoFullName);
                    // Try to discover specs from the repository's file tree
                    discoverSpecsFromRepo(repoFullName, headSha, changedSpecs);
                }
            }

            // Step 5: Fetch and ingest each changed spec
            int ingested = 0;
            int failed = 0;
            for (SpecChange change : changedSpecs) {
                try {
                    String content = fetchSpecContent(repoFullName, change.path, change.commitSha);
                    if (content != null && !content.isBlank()) {
                        var incomingSpec = new IncomingSpec(repoFullName, change.commitSha, change.path, content);
                        var result = ingestionService.ingestSpec(incomingSpec);
                        ingested++;
                        log.info(
                                "Ingested spec {} (commit {}) via webhook: eventId={}",
                                change.path,
                                change.commitSha,
                                result.eventId());
                    }
                } catch (Exception e) {
                    failed++;
                    log.warn("Failed to ingest spec {} from webhook: {}", change.path, e.getMessage());
                }
            }

            log.info(
                    "Webhook processed: repo={}, branch={}, deliveryId={}, ingested={}, failed={}",
                    repoFullName,
                    branch,
                    deliveryId,
                    ingested,
                    failed);

            return deliveryId;

        } catch (JsonProcessingException e) {
            throw new WebhookValidationException(deliveryId.toString(), "Malformed JSON payload", e);
        }
    }

    /**
     * Fetches the raw content of a spec file from the GitHub API.
     */
    private String fetchSpecContent(String repo, String path, String commitSha) {
        try {
            String apiUrl = String.format("https://api.github.com/repos/%s/contents/%s?ref=%s", repo, path, commitSha);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github.raw+json");

            if (githubToken != null && !githubToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + githubToken);
            }

            HttpResponse<String> response =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                log.warn("GitHub API returned {} for {}/{} at {}", response.statusCode(), repo, path, commitSha);
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch spec content from GitHub API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Discovers spec files in a repository by querying the GitHub API tree.
     */
    private void discoverSpecsFromRepo(String repo, String commitSha, List<SpecChange> changes) {
        try {
            String apiUrl = String.format("https://api.github.com/repos/%s/git/trees/%s?recursive=1", repo, commitSha);

            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder().uri(URI.create(apiUrl)).header("Accept", "application/vnd.github+json");

            if (githubToken != null && !githubToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + githubToken);
            }

            HttpResponse<String> response =
                    httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode tree = objectMapper.readTree(response.body()).get("tree");
                if (tree != null && tree.isArray()) {
                    for (JsonNode entry : tree) {
                        String path = entry.has("path") ? entry.get("path").asText() : "";
                        if (isOpenApiSpec(path)) {
                            changes.add(new SpecChange(path, commitSha));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to discover specs from repository tree: {}", e.getMessage());
        }
    }

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
            return (SIGNATURE_PREFIX + hexString).equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC computation failed", e);
            return false;
        }
    }

    private void extractSpecPaths(
            JsonNode commit, String field, String repo, String commitSha, List<SpecChange> changes) {
        JsonNode files = commit.get(field);
        if (files != null && files.isArray()) {
            for (JsonNode file : files) {
                String path = file.asText();
                if (isOpenApiSpec(path)) {
                    changes.add(new SpecChange(path, commitSha));
                    log.info("Detected spec change in {}: {} (commit {}, repo {})", field, path, commitSha, repo);
                }
            }
        }
    }

    private boolean isOpenApiSpec(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".json");
    }

    /**
     * A detected spec file change from a webhook payload.
     */
    private record SpecChange(String path, String commitSha) {}
}
