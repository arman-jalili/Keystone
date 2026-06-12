package com.keystone.ingestion.interfaces.http;

import com.keystone.ingestion.application.dto.IdempotencyCheckRequest;
import com.keystone.ingestion.application.dto.IncomingSpec;
import com.keystone.ingestion.application.dto.SpecIngestedResponse;
import com.keystone.ingestion.application.service.IngestionService;
import com.keystone.ingestion.domain.exception.DuplicateSpecException;
import com.keystone.ingestion.domain.exception.SpecParseException;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST API contract for the Ingestion bounded context.
 *
 * <p>Handles incoming spec uploads from the Keystone CLI and webhook events
 * from GitHub/GitLab. This is the primary entry point into the module.
 *
 * <h3>Endpoint Contract</h3>
 * <pre>
 * POST   /api/v1/ingestion/audit          — Upload a spec for audit
 * POST   /api/v1/ingestion/webhook/github  — GitHub webhook receiver
 * GET    /api/v1/ingestion/idempotency     — Pre-flight idempotency check
 * </pre>
 *
 * <h3>HTTP Status Codes</h3>
 * <ul>
 *   <li><strong>201 Created</strong> — Spec successfully ingested</li>
 *   <li><strong>200 OK</strong> — Spec already processed (idempotent duplicate)</li>
 *   <li><strong>400 Bad Request</strong> — Invalid payload (validation failure)</li>
 *   <li><strong>401 Unauthorized</strong> — Missing or invalid authentication</li>
 *   <li><strong>422 Unprocessable Entity</strong> — Spec content failed validation</li>
 *   <li><strong>503 Service Unavailable</strong> — Database or upstream failure</li>
 * </ul>
 *
 * <p>Note: This file defines the API contract only. The actual implementation
 * (with {@code @RestController} and annotations) will be provided by the
 * implementation issue.
 */
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * POST /api/v1/ingestion/audit
     *
     * <p>Ingests an OpenAPI specification for audit analysis.
     *
     * <p><strong>Request:</strong></p>
     * <pre>
     * POST /api/v1/ingestion/audit
     * Content-Type: application/json
     * Authorization: Bearer {token}
     *
     * {
     *   "repository": "org/repo",
     *   "commitSha": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0",
     *   "specPath": "openapi/checkout.yaml",
     *   "content": "openapi: 3.0.0\n..."
     * }
     * </pre>
     *
     * <p><strong>Response (201 Created):</strong></p>
     * <pre>
     * {
     *   "eventId": "uuid",
     *   "specId": "uuid",
     *   "repository": "org/repo",
     *   "specPath": "openapi/checkout.yaml",
     *   "commitSha": "a1b2c3...",
     *   "checksum": "sha256hex",
     *   "ingestedAt": "2026-06-12T10:30:00Z"
     * }
     * </pre>
     *
     * <p><strong>Response (200 OK - duplicate):</strong></p>
     * <pre>
     * {
     *   "eventId": "uuid",
     *   "specId": "uuid",
     *   "repository": "org/repo",
     *   "specPath": "openapi/checkout.yaml",
     *   "commitSha": "a1b2c3...",
     *   "checksum": "sha256hex",
     *   "ingestedAt": "2026-06-12T10:30:00Z"
     * }
     * </pre>
     *
     * @param request the incoming spec payload
     * @return 201 Created for new spec, 200 OK for duplicate
     */
    @PostMapping(path = "/api/v1/ingestion/audit",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpecIngestedResponse> ingestSpec(@Valid @RequestBody IncomingSpec request) {
        SpecIngestedResponse response = ingestionService.ingestSpec(request);
        // The service returns the response; controller determines the status code.
        // TODO: Determine new vs. duplicate — need to pass an indicator from the service.
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/ingestion/webhook/github
     *
     * <p>Receives GitHub push webhook events. The webhook payload is parsed
     * to extract spec files, then delegated to the ingestion service.
     *
     * <p><strong>Request:</strong></p>
     * <pre>
     * POST /api/v1/ingestion/webhook/github
     * Content-Type: application/json
     * X-Hub-Signature-256: {hmac-sha256-signature}
     * X-GitHub-Event: push
     * X-GitHub-Delivery: {uuid}
     *
     * { ... full GitHub push event payload ... }
     * </pre>
     *
     * <p><strong>Response (202 Accepted):</strong></p>
     * <pre>
     * {
     *   "accepted": true,
     *   "deliveryId": "uuid"
     * }
     * </pre>
     *
     * @param payload   the raw GitHub webhook payload
     * @param signature the X-Hub-Signature-256 header value
     * @return 202 Accepted if the webhook was accepted for processing
     */
    @PostMapping(path = "/api/v1/ingestion/webhook/github",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookAcceptedResponse> handleGitHubWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Hub-Signature-256") String signature) {
        // Webhook processing is async — acknowledge immediately, process later.
        UUID deliveryId = UUID.randomUUID();
        // TODO: Validate signature, extract spec paths, queue for async ingestion
        return ResponseEntity.accepted().body(new WebhookAcceptedResponse(true, deliveryId));
    }

    /**
     * GET /api/v1/ingestion/idempotency?repository={repo}&amp;commitSha={sha}&amp;specPath={path}
     *
     * <p>Pre-flight idempotency check. Allows the CLI to check whether a spec
     * has already been ingested without uploading the full content.
     *
     * <p><strong>Response (200 OK - exists):</strong></p>
     * <pre>
     * {
     *   "duplicate": true,
     *   "existingEventId": "uuid"
     * }
     * </pre>
     *
     * <p><strong>Response (200 OK - new):</strong></p>
     * <pre>
     * {
     *   "duplicate": false
     * }
     * </pre>
     *
     * @param request the idempotency check parameters
     * @return whether the spec has already been ingested
     */
    @GetMapping(path = "/api/v1/ingestion/idempotency",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IdempotencyCheckResponse> checkIdempotency(
            @Valid @ModelAttribute IdempotencyCheckRequest request) {
        Optional<UUID> existingEventId = ingestionService.checkIdempotency(request);
        return existingEventId
            .map(id -> ResponseEntity.ok(new IdempotencyCheckResponse(true, id)))
            .orElseGet(() -> ResponseEntity.ok(new IdempotencyCheckResponse(false, null)));
    }

    // --- Response types ---

    /**
     * Response for webhook acceptance.
     */
    public record WebhookAcceptedResponse(boolean accepted, UUID deliveryId) {}

    /**
     * Response for idempotency check.
     */
    public record IdempotencyCheckResponse(boolean duplicate, UUID existingEventId) {}
}
