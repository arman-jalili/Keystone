package com.keystone.policy.interfaces.http;

import com.keystone.policy.application.dto.*;
import com.keystone.policy.application.service.PolicyEvaluationService;
import com.keystone.policy.application.service.PolicyManagementService;
import com.keystone.policy.application.service.PolicySyncService;
import com.keystone.policy.domain.exception.PolicyEvaluationException;
import com.keystone.policy.domain.exception.PolicyNotFoundException;
import com.keystone.policy.domain.exception.PolicyParseException;
import com.keystone.policy.domain.exception.PolicySyncException;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyStatus;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Policy Engine bounded context.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /api/v1/policies/evaluate} — Evaluate a spec against policies</li>
 *   <li>{@code GET /api/v1/policies/evaluations/{evaluationId}} — Get an evaluation result</li>
 *   <li>{@code GET /api/v1/policies} — List policies</li>
 *   <li>{@code POST /api/v1/policies} — Create a policy</li>
 *   <li>{@code GET /api/v1/policies/{policyId}} — Get a policy</li>
 *   <li>{@code PUT /api/v1/policies/{policyId}} — Update a policy</li>
 *   <li>{@code DELETE /api/v1/policies/{policyId}} — Deactivate a policy</li>
 *   <li>{@code POST /api/v1/policies/{policyId}/activate} — Reactivate a policy</li>
 *   <li>{@code POST /api/v1/policies/sync} — Sync policies from source</li>
 *   <li>{@code POST /api/v1/policies/sources} — Configure a policy source</li>
 *   <li>{@code DELETE /api/v1/policies/sources/{sourceId}} — Remove a policy source</li>
 *   <li>{@code GET /api/v1/policies/sources} — List policy sources</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private static final Logger log = LoggerFactory.getLogger(PolicyController.class);

    private final PolicyEvaluationService evaluationService;
    private final PolicyManagementService managementService;
    private final PolicySyncService syncService;

    public PolicyController(PolicyEvaluationService evaluationService,
                            PolicyManagementService managementService,
                            PolicySyncService syncService) {
        this.evaluationService = evaluationService;
        this.managementService = managementService;
        this.syncService = syncService;
    }

    // ---- Evaluation endpoints ----

    /**
     * POST /api/v1/policies/evaluate
     *
     * <p>Evaluates an OpenAPI specification against the applicable policies.
     *
     * @param request the evaluation request payload
     * @return 200 OK with the evaluation results
     */
    @PostMapping(path = "/evaluate",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluateSpecResponse> evaluateSpec(
            @Valid @RequestBody EvaluateSpecRequest request) {
        try {
            EvaluateSpecResponse response = evaluationService.evaluateSpec(request);
            return ResponseEntity.ok(response);
        } catch (PolicyNotFoundException e) {
            log.warn("Evaluation requested for unknown policy set: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (PolicyEvaluationException e) {
            log.error("Evaluation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    /**
     * GET /api/v1/policies/evaluations/{evaluationId}
     *
     * <p>Retrieves a previously completed evaluation result.
     *
     * @param evaluationId the UUID of the evaluation result
     * @return 200 OK with the evaluation result, or 404 if not found
     */
    @GetMapping(path = "/evaluations/{evaluationId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluateSpecResponse> getEvaluationResult(
            @PathVariable("evaluationId") UUID evaluationId) {
        try {
            EvaluateSpecResponse response = evaluationService.getEvaluationResult(evaluationId);
            return ResponseEntity.ok(response);
        } catch (PolicyNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- CRUD endpoints ----

    /**
     * POST /api/v1/policies
     *
     * <p>Creates a new policy rule.
     *
     * @param request the policy creation payload
     * @return 201 Created with the created policy summary
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicySummaryResponse> createPolicy(
            @Valid @RequestBody CreatePolicyRequest request) {
        try {
            Policy policy = managementService.createPolicy(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(PolicySummaryResponse.from(policy));
        } catch (PolicyParseException e) {
            log.warn("Policy creation failed due to parse error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    /**
     * GET /api/v1/policies
     *
     * <p>Lists all policies, optionally filtered by status.
     *
     * @param status   optional status filter (active, inactive, deprecated)
     * @param sourceId optional source filter
     * @return 200 OK with the list of policy summaries
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PolicySummaryResponse>> listPolicies(
            @RequestParam(value = "status", required = false) PolicyStatus status,
            @RequestParam(value = "sourceId", required = false) String sourceId) {
        List<PolicySummaryResponse> policies = managementService.listPolicies(status, sourceId);
        return ResponseEntity.ok(policies);
    }

    /**
     * GET /api/v1/policies/{policyId}
     *
     * <p>Retrieves a single policy by its UUID.
     *
     * @param policyId the UUID of the policy
     * @return 200 OK with the policy, or 404 if not found
     */
    @GetMapping(path = "/{policyId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicySummaryResponse> getPolicy(
            @PathVariable("policyId") UUID policyId) {
        try {
            Policy policy = managementService.getPolicy(policyId);
            return ResponseEntity.ok(PolicySummaryResponse.from(policy));
        } catch (PolicyNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PUT /api/v1/policies/{policyId}
     *
     * <p>Updates an existing policy.
     *
     * @param policyId the UUID of the policy to update
     * @param request  the fields to update
     * @return 200 OK with the updated policy, or 404 if not found
     */
    @PutMapping(path = "/{policyId}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicySummaryResponse> updatePolicy(
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody UpdatePolicyRequest request) {
        try {
            Policy policy = managementService.updatePolicy(policyId, request);
            return ResponseEntity.ok(PolicySummaryResponse.from(policy));
        } catch (PolicyNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (PolicyParseException e) {
            log.warn("Policy update failed due to parse error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    /**
     * DELETE /api/v1/policies/{policyId}
     *
     * <p>Deactivates a policy (soft delete).
     *
     * @param policyId the UUID of the policy to deactivate
     * @return 204 No Content, or 404 if not found
     */
    @DeleteMapping(path = "/{policyId}")
    public ResponseEntity<Void> deactivatePolicy(
            @PathVariable("policyId") UUID policyId) {
        try {
            managementService.deactivatePolicy(policyId);
            return ResponseEntity.noContent().build();
        } catch (PolicyNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/v1/policies/{policyId}/activate
     *
     * <p>Reactivates a previously deactivated policy.
     *
     * @param policyId the UUID of the policy to activate
     * @return 200 OK with the activated policy, or 404 if not found
     */
    @PostMapping(path = "/{policyId}/activate",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicySummaryResponse> activatePolicy(
            @PathVariable("policyId") UUID policyId) {
        try {
            Policy policy = managementService.activatePolicy(policyId);
            return ResponseEntity.ok(PolicySummaryResponse.from(policy));
        } catch (PolicyNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- Sync endpoints ----

    /**
     * POST /api/v1/policies/sync
     *
     * <p>Triggers a policy synchronization from an external source.
     *
     * @param request the sync request payload
     * @return 200 OK with the sync result
     */
    @PostMapping(path = "/sync",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SyncPoliciesResponse> syncPolicies(
            @Valid @RequestBody SyncPoliciesRequest request) {
        try {
            SyncPoliciesResponse response = syncService.syncPolicies(request);
            if (response.success()) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (PolicySyncException e) {
            log.error("Policy sync failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SyncPoliciesResponse.failure(request.sourceId(), e.getMessage()));
        }
    }

    /**
     * POST /api/v1/policies/sources
     *
     * <p>Registers or updates a policy source configuration.
     *
     * @param request the source configuration payload
     * @return 201 Created if new source, 200 OK if updated
     */
    @PostMapping(path = "/sources",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> configureSource(
            @Valid @RequestBody PolicySourceConfigRequest request) {
        try {
            syncService.configureSource(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (PolicySyncException e) {
            log.error("Failed to configure policy source: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    /**
     * DELETE /api/v1/policies/sources/{sourceId}
     *
     * <p>Removes a policy source configuration.
     *
     * @param sourceId the identifier of the source to remove
     * @return 204 No Content
     */
    @DeleteMapping(path = "/sources/{sourceId}")
    public ResponseEntity<Void> removeSource(
            @PathVariable("sourceId") String sourceId) {
        try {
            syncService.removeSource(sourceId, false);
            return ResponseEntity.noContent().build();
        } catch (PolicySyncException e) {
            log.error("Failed to remove policy source: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/policies/sources
     *
     * <p>Lists all configured policy source identifiers.
     *
     * @return 200 OK with the list of source IDs
     */
    @GetMapping(path = "/sources",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> listSources() {
        List<String> sources = syncService.listSources();
        return ResponseEntity.ok(sources);
    }
}
