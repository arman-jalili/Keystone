// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.interfaces.http;

import com.keystone.ingestion.application.dto.IdempotencyCheckRequest;
import com.keystone.ingestion.application.dto.IncomingSpec;
import com.keystone.ingestion.application.dto.SpecIngestedResponse;
import com.keystone.ingestion.application.service.IngestionService;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Ingestion bounded context.
 *
 * <p>Handles incoming spec uploads from the Keystone CLI and webhook events
 * from GitHub/GitLab. This is the primary entry point into the module.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /api/v1/ingestion/audit} — Upload a spec for audit</li>
 *   <li>{@code POST /api/v1/ingestion/webhook/github} — GitHub webhook receiver</li>
 *   <li>{@code GET /api/v1/ingestion/idempotency} — Pre-flight dedup check</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ingestion")
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
     * @param request the incoming spec payload
     * @return 201 Created for new spec, 200 OK for duplicate
     */
    @PostMapping(
            path = "/audit",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpecIngestedResponse> ingestSpec(@Valid @RequestBody IncomingSpec request) {
        SpecIngestedResponse response = ingestionService.ingestSpec(request);
        if (response.duplicate()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/ingestion/webhook/github
     *
     * <p>Receives GitHub push webhook events. Acknowledges immediately
     * and processes asynchronously.
     *
     * @param payload   the raw GitHub webhook payload
     * @param signature the X-Hub-Signature-256 header value
     * @return 202 Accepted if the webhook was accepted for processing
     */
    @PostMapping(
            path = "/webhook/github",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookAcceptedResponse> handleGitHubWebhook(
            @RequestBody String payload, @RequestHeader("X-Hub-Signature-256") String signature) {
        UUID deliveryId = UUID.randomUUID();
        // TODO: Validate webhook signature, extract spec, queue for async processing
        return ResponseEntity.accepted().body(new WebhookAcceptedResponse(true, deliveryId));
    }

    /**
     * GET /api/v1/ingestion/idempotency
     *
     * <p>Pre-flight idempotency check. Allows the CLI to check whether a spec
     * has already been ingested without uploading the full content.
     *
     * @param request the idempotency check parameters
     * @return whether the spec has already been ingested
     */
    @GetMapping(path = "/idempotency", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IdempotencyCheckResponse> checkIdempotency(
            @Valid @ModelAttribute IdempotencyCheckRequest request) {
        Optional<UUID> existingEventId = ingestionService.checkIdempotency(request);
        return existingEventId
                .map(id -> ResponseEntity.ok(new IdempotencyCheckResponse(true, id)))
                .orElseGet(() -> ResponseEntity.ok(new IdempotencyCheckResponse(false, null)));
    }

    /** Response for webhook acceptance. */
    public record WebhookAcceptedResponse(boolean accepted, UUID deliveryId) {}

    /** Response for idempotency check. */
    public record IdempotencyCheckResponse(boolean duplicate, UUID existingEventId) {}
}
