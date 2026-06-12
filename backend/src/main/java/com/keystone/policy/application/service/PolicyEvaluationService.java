package com.keystone.policy.application.service;

import com.keystone.policy.application.dto.EvaluateSpecRequest;
import com.keystone.policy.application.dto.EvaluateSpecResponse;
import com.keystone.policy.domain.exception.PolicyEvaluationException;
import com.keystone.policy.domain.exception.PolicyNotFoundException;
import com.keystone.policy.domain.service.EvaluationEngine;

/**
 * Application service for evaluating OpenAPI specifications against policies.
 *
 * <p>This is the primary inbound port (driving adapter) for policy evaluation.
 * The {@link com.keystone.policy.interfaces.http.PolicyController} depends on
 * this interface.
 *
 * <p>Orchestrates the evaluation flow:
 * <ol>
 *   <li>Load the appropriate policy set via {@link com.keystone.policy.infrastructure.repository.PolicyRepository}</li>
 *   <li>Evaluate the specification against active policies via {@link EvaluationEngine}</li>
 *   <li>Persist the {@link com.keystone.policy.domain.model.PolicyEvaluationResult}</li>
 *   <li>Publish a {@link com.keystone.policy.domain.event.PolicyEvaluatedEvent}</li>
 * </ol>
 */
public interface PolicyEvaluationService {

    /**
     * Evaluates an OpenAPI specification against the applicable policy set(s).
     *
     * <p>If a specific {@code policySetId} was provided in the request,
     * only that policy set is evaluated. Otherwise, all active policy sets
     * are evaluated against the spec.
     *
     * @param request the evaluation request with spec details
     * @return the evaluation result with violations and verdict
     * @throws PolicyEvaluationException if the evaluation pipeline fails
     * @throws PolicyNotFoundException   if the specified policy set does not exist
     */
    EvaluateSpecResponse evaluateSpec(EvaluateSpecRequest request)
            throws PolicyEvaluationException, PolicyNotFoundException;

    /**
     * Retrieves a previously completed evaluation result by ID.
     *
     * @param evaluationId the UUID of the evaluation result
     * @return the evaluation result
     * @throws PolicyNotFoundException if the evaluation result is not found
     */
    EvaluateSpecResponse getEvaluationResult(java.util.UUID evaluationId)
            throws PolicyNotFoundException;
}
