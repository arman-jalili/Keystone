// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.evaluator;

import com.keystone.policy.application.dto.EvaluateSpecRequest;
import com.keystone.policy.application.dto.EvaluateSpecResponse;
import com.keystone.policy.application.service.PolicyEvaluationService;
import com.keystone.policy.domain.event.PolicyEvaluatedEvent;
import com.keystone.policy.domain.exception.PolicyEvaluationException;
import com.keystone.policy.domain.exception.PolicyNotFoundException;
import com.keystone.policy.domain.model.PolicyEvaluationResult;
import com.keystone.policy.domain.model.PolicySet;
import com.keystone.policy.domain.service.EvaluationEngine;
import com.keystone.policy.infrastructure.event.PolicyEventPublisher;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the policy evaluation flow.
 *
 * <p>Coordinates policy set loading, evaluation, persistence, and event publication.
 */
@Service
@Transactional
public class PolicyEvaluationServiceImpl implements PolicyEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationServiceImpl.class);

    private final PolicyRepository policyRepository;
    private final EvaluationEngine evaluationEngine;
    private final PolicyEventPublisher eventPublisher;

    public PolicyEvaluationServiceImpl(
            PolicyRepository policyRepository, EvaluationEngine evaluationEngine, PolicyEventPublisher eventPublisher) {
        this.policyRepository = policyRepository;
        this.evaluationEngine = evaluationEngine;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public EvaluateSpecResponse evaluateSpec(EvaluateSpecRequest request)
            throws PolicyEvaluationException, PolicyNotFoundException {

        log.info(
                "Evaluating spec at {}/{} (commit: {})", request.repository(), request.specPath(), request.commitSha());

        // Determine which policy set to evaluate
        List<PolicySet> policySets;
        if (request.hasSpecificPolicySet()) {
            // Evaluate a specific policy set
            PolicySet policySet = policyRepository
                    .findPolicySetById(request.policySetId())
                    .orElseThrow(() -> new PolicyNotFoundException(request.policySetId()));
            policySets = List.of(policySet);
        } else {
            // Evaluate all policy sets
            policySets = policyRepository.findAllPolicySets();
            if (policySets.isEmpty()) {
                log.warn("No policy sets configured for evaluation");
                // Return a PASS verdict with no violations
                PolicyEvaluationResult emptyResult = new PolicyEvaluationResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        request.repository(),
                        request.specPath(),
                        request.commitSha(),
                        PolicyEvaluationResult.Verdict.PASS,
                        List.of(),
                        0,
                        0,
                        0,
                        Instant.now());
                return EvaluateSpecResponse.from(policyRepository.saveEvaluation(emptyResult));
            }
        }

        // Evaluate each policy set and aggregate results
        PolicyEvaluationResult combinedResult = null;
        for (PolicySet policySet : policySets) {
            // Use a placeholder specId - in production this would be
            // the actual spec UUID resolved from the repository
            UUID specId = UUID.nameUUIDFromBytes((request.repository() + ":" + request.specPath()).getBytes());

            PolicyEvaluationResult result = evaluationEngine.evaluate(policySet, specId);
            combinedResult = result; // Take the last result for now

            // Publish event for each evaluation
            var evaluatedEvent = new PolicyEvaluatedEvent(
                    UUID.randomUUID(),
                    specId,
                    policySet.getId(),
                    request.repository(),
                    request.specPath(),
                    request.commitSha(),
                    result.getVerdict(),
                    result.getViolations().size(),
                    Instant.now());
            eventPublisher.policyEvaluated(evaluatedEvent);
        }

        return EvaluateSpecResponse.from(combinedResult);
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluateSpecResponse getEvaluationResult(UUID evaluationId) throws PolicyNotFoundException {
        return policyRepository
                .findEvaluationById(evaluationId)
                .map(EvaluateSpecResponse::from)
                .orElseThrow(() -> new PolicyNotFoundException(evaluationId));
    }
}
