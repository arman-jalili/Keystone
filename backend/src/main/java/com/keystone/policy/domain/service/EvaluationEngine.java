package com.keystone.policy.domain.service;

import com.keystone.policy.domain.exception.PolicyEvaluationException;
import com.keystone.policy.domain.model.PolicyEvaluationResult;
import com.keystone.policy.domain.model.PolicySet;

import java.util.UUID;

/**
 * Domain service that evaluates OpenAPI specifications against a set of policies.
 *
 * <p>Per the policy-engine architecture, orchestrates the following workflow:
 * <ol>
 *   <li>Load the policy set from the repository</li>
 *   <li>Resolve policy scope against the spec endpoints</li>
 *   <li>Evaluate each active policy's DSL expression against matching spec elements</li>
 *   <li>Collect violations and compute the overall verdict</li>
 *   <li>Persist the {@link PolicyEvaluationResult}</li>
 *   <li>Publish a {@link com.keystone.policy.domain.event.PolicyEvaluatedEvent}</li>
 * </ol>
 *
 * <p>Implementations must be stateless and thread-safe.
 */
public interface EvaluationEngine {

    /**
     * Evaluates all active policies in the given policy set against
     * a specification identified by its spec ID.
     *
     * @param policySet  the policy set to evaluate
     * @param specId     the UUID of the OpenAPI specification to evaluate
     * @return the complete evaluation result with violations and verdict
     * @throws PolicyEvaluationException if the evaluation pipeline fails
     */
    PolicyEvaluationResult evaluate(PolicySet policySet, UUID specId)
            throws PolicyEvaluationException;

    /**
     * Evaluates a subset of policies (by policy ID) from a policy set
     * against a specification. Used for targeted re-evaluation.
     *
     * @param policySet  the policy set containing the policies
     * @param specId     the UUID of the specification to evaluate
     * @param policyIds  the specific policies to evaluate (must belong to the set)
     * @return the complete evaluation result for the subset
     * @throws PolicyEvaluationException if the evaluation pipeline fails
     */
    PolicyEvaluationResult evaluateSubset(PolicySet policySet, UUID specId,
                                           java.util.Set<java.util.UUID> policyIds)
            throws PolicyEvaluationException;
}
