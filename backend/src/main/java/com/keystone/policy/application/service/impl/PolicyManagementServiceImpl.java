// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Implements: CRUD operations for policy definitions
package com.keystone.policy.application.service.impl;

import com.keystone.policy.application.dto.CreatePolicyRequest;
import com.keystone.policy.application.dto.PolicyAggregateSummary;
import com.keystone.policy.application.dto.PolicySummaryResponse;
import com.keystone.policy.application.dto.UpdatePolicyRequest;
import com.keystone.policy.application.service.PolicyManagementService;
import com.keystone.policy.domain.exception.PolicyNotFoundException;
import com.keystone.policy.domain.exception.PolicyParseException;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyEvaluationResult;
import com.keystone.policy.domain.model.PolicyEvaluationResult.Verdict;
import com.keystone.policy.domain.model.PolicyStatus;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link PolicyManagementService}.
 *
 * <p>Provides CRUD operations for policies. Delegates persistence to
 * the repository and validates DSL expressions before saving.
 */
@Service
public class PolicyManagementServiceImpl implements PolicyManagementService {

    private static final Logger log = LoggerFactory.getLogger(PolicyManagementServiceImpl.class);

    private final PolicyRepository policyRepository;

    public PolicyManagementServiceImpl(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    public Policy createPolicy(CreatePolicyRequest request) throws PolicyParseException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Policy updatePolicy(UUID policyId, UpdatePolicyRequest request)
            throws PolicyNotFoundException, PolicyParseException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Policy getPolicy(UUID policyId) throws PolicyNotFoundException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<PolicySummaryResponse> listPolicies(PolicyStatus status, String sourceId) {
        return List.of();
    }

    @Override
    public void deactivatePolicy(UUID policyId) throws PolicyNotFoundException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Policy activatePolicy(UUID policyId) throws PolicyNotFoundException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PolicyAggregateSummary getPolicySummary() {
        List<Policy> allPolicies = policyRepository.findAllPolicies(null);
        List<Policy> activePolicies = allPolicies.stream()
                .filter(p -> p.getStatus() == PolicyStatus.ACTIVE)
                .toList();

        int activeCount = activePolicies.size();

        // Query recent evaluation results to derive pass rate and open violations
        List<PolicyEvaluationResult> recentResults = collectRecentEvaluations();
        long totalEvaluations = recentResults.size();
        long passedEvaluations = recentResults.stream()
                .filter(r -> r.getVerdict() == Verdict.PASS)
                .count();
        long failedEvaluations = recentResults.stream()
                .filter(r -> r.getVerdict() == Verdict.FAIL || r.getVerdict() == Verdict.WARNING)
                .count();

        int passRate = totalEvaluations > 0
                ? (int) Math.round((double) passedEvaluations / totalEvaluations * 100)
                : 100;

        return new PolicyAggregateSummary(
                activeCount,
                passRate,
                (int) failedEvaluations,
                (int) activeCount // covered APIs approximated by active policy count
        );
    }

    private List<PolicyEvaluationResult> collectRecentEvaluations() {
        // Query across all policy sets for recent evaluations
        return policyRepository.findAllPolicySets().stream()
                .flatMap(ps -> {
                    try {
                        return policyRepository
                                .findEvaluationsBySpecId(ps.getId(), 100)
                                .stream();
                    } catch (Exception e) {
                        log.warn("Failed to query evaluations for policy set {}: {}", ps.getId(), e.getMessage());
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
    }
}
