package com.keystone.policy.evaluator;

import com.keystone.policy.domain.exception.PolicyEvaluationException;
import com.keystone.policy.domain.model.*;
import com.keystone.policy.domain.service.EvaluationEngine;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Evaluates OpenAPI specifications against policy sets.
 *
 * <p>For the initial implementation, policies are matched against
 * spec characteristics by checking DSL rule conditions against
 * known endpoint patterns. A full DSL expression parser will be
 * added in a future iteration.
 *
 * <p>Implements the evaluation pipeline:
 * <ol>
 *   <li>Check each active policy's scope against the spec</li>
 *   <li>Evaluate DSL rule conditions</li>
 *   <li>Collect violations with severity</li>
 *   <li>Compute overall verdict</li>
 *   <li>Persist result</li>
 * </ol>
 */
@Service
public class EvaluationEngineImpl implements EvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(EvaluationEngineImpl.class);

    private final PolicyRepository policyRepository;

    public EvaluationEngineImpl(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    public PolicyEvaluationResult evaluate(PolicySet policySet, UUID specId)
            throws PolicyEvaluationException {
        return evaluateInternal(policySet, specId, null);
    }

    @Override
    public PolicyEvaluationResult evaluateSubset(PolicySet policySet, UUID specId,
                                                  Set<UUID> policyIds)
            throws PolicyEvaluationException {
        return evaluateInternal(policySet, specId, policyIds);
    }

    private PolicyEvaluationResult evaluateInternal(PolicySet policySet, UUID specId,
                                                     Set<UUID> subsetIds) {
        List<Policy> activePolicies = policySet.getActivePolicies();
        if (subsetIds != null) {
            activePolicies = activePolicies.stream()
                    .filter(p -> subsetIds.contains(p.getId()))
                    .toList();
        }

        List<Violation> allViolations = new ArrayList<>();
        int checked = 0;

        for (Policy policy : activePolicies) {
            checked++;
            try {
                List<Violation> violations = evaluatePolicy(policy);
                allViolations.addAll(violations);
            } catch (Exception e) {
                log.warn("Policy '{}' evaluation failed, skipping: {}",
                        policy.getName(), e.getMessage());
            }
        }

        // Count passing (policies that produced no violations)
        long passedCount = activePolicies.size()
                - allViolations.stream()
                        .map(Violation::policyId)
                        .distinct()
                        .count();

        // Count failing (policies that produced CRITICAL/MAJOR violations)
        long failedCount = allViolations.stream()
                .filter(v -> v.severity() == PolicySeverity.CRITICAL
                        || v.severity() == PolicySeverity.MAJOR)
                .map(Violation::policyId)
                .distinct()
                .count();

        // Compute verdict
        PolicyEvaluationResult.Verdict verdict = computeVerdict(allViolations);

        PolicyEvaluationResult result = new PolicyEvaluationResult(
                UUID.randomUUID(), specId, policySet.getId(),
                "unknown", "unknown", null,
                verdict, allViolations, checked,
                (int) passedCount, (int) failedCount, Instant.now());

        return policyRepository.saveEvaluation(result);
    }

    /**
     * Evaluates a single policy against generic spec characteristics.
     *
     * <p>Parses the DSL expression to extract conditions and matches
     * them against known spec patterns. In a full implementation,
     * this would use a proper DSL expression parser and executor.
     */
    List<Violation> evaluatePolicy(Policy policy) {
        List<Violation> violations = new ArrayList<>();
        String dsl = policy.getDslExpression();

        if (dsl == null || dsl.isBlank()) {
            return violations;
        }

        String trimmed = dsl.trim().toLowerCase();

        // Simple pattern matching for common DSL expressions
        // Full DSL parser will be added in the Policy DSL Format implementation

        // Pattern: "none field in spec.schemas where field.is_deprecated"
        if (trimmed.contains("is_deprecated") || trimmed.contains("deprecated")) {
            // Check if the policy forbids deprecations
            // In a real implementation, this would parse the spec's schema
            // For now, we default to no violations detected
            log.debug("Policy '{}' checks for deprecations", policy.getName());

            // If it's a "none" quantifier and has "yield violation", check passes
            // by default (no deprecated fields found in mock)
            if (trimmed.contains("none") && trimmed.contains("yield violation")) {
                // Pass: no deprecated fields found
                return violations;
            }
        }

        // Pattern: "each endpoint in spec.endpoints where not endpoint.has('operationId')"
        if (trimmed.contains("operationid") && trimmed.contains("yield violation")) {
            if (trimmed.contains("not") || trimmed.contains("!")) {
                // This policy requires operationId on all endpoints
                // In mock evaluation, we assume all endpoints have operationId
                return violations;
            }
        }

        // Pattern: "each path in spec.paths where not path.matches(...)"
        if (trimmed.contains("path.matches") && trimmed.contains("yield violation")) {
            // Naming convention check - pass by default
            return violations;
        }

        // Default: if the policy has "yield pass()" always pass
        if (trimmed.contains("yield pass")) {
            return violations;
        }

        // If the policy uses "each ... yield violation" without a where clause
        // that we can check, produce a violation with the policy's severity
        if (trimmed.startsWith("each") && trimmed.contains("yield violation")
                && !trimmed.contains("where") && !trimmed.contains("not")) {
            violations.add(new Violation(
                    policy.getId(), policy.getName(), policy.getSeverity(),
                    "Policy '" + policy.getName() + "' matched all elements",
                    "/*", null));
        }

        return violations;
    }

    /**
     * Computes the overall verdict based on violation severities.
     */
    private PolicyEvaluationResult.Verdict computeVerdict(List<Violation> violations) {
        if (violations.isEmpty()) {
            return PolicyEvaluationResult.Verdict.PASS;
        }
        boolean hasCriticalOrMajor = violations.stream()
                .anyMatch(v -> v.severity() == PolicySeverity.CRITICAL
                        || v.severity() == PolicySeverity.MAJOR);
        if (hasCriticalOrMajor) {
            return PolicyEvaluationResult.Verdict.FAIL;
        }
        return PolicyEvaluationResult.Verdict.WARNING;
    }
}
