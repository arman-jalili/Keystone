package com.keystone.policy.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.policy.domain.model.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(PolicyRepositoryImpl.class)
@ActiveProfiles("test")
class PolicyRepositoryImplTest {

    @Autowired
    private PolicyRepository policyRepository;

    private final Instant now = Instant.now();

    private Policy createPolicy(String name, PolicySeverity severity, PolicyStatus status) {
        return new Policy(
                UUID.randomUUID(),
                name,
                "Test " + name,
                severity,
                status,
                PolicyScope.all(),
                "each endpoint in spec.endpoints yield pass()",
                "test-source",
                1,
                now,
                now);
    }

    @BeforeEach
    void setUp() {
        // Ensure clean state
        policyRepository.findAllPolicies(null).forEach(p -> policyRepository.deletePolicy(p.getId()));
    }

    // ---- Policy CRUD ----

    @Test
    void findPolicyById_shouldReturnEmptyForUnknownId() {
        Optional<Policy> result = policyRepository.findPolicyById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void saveAndFindPolicyById_shouldRoundTrip() {
        Policy policy = createPolicy("test-roundtrip", PolicySeverity.MAJOR, PolicyStatus.ACTIVE);

        Policy saved = policyRepository.savePolicy(policy);
        assertThat(saved.getId()).isEqualTo(policy.getId());

        Optional<Policy> found = policyRepository.findPolicyById(policy.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-roundtrip");
        assertThat(found.get().getSeverity()).isEqualTo(PolicySeverity.MAJOR);
        assertThat(found.get().getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(found.get().getDslExpression()).isEqualTo(policy.getDslExpression());
    }

    @Test
    void findPolicyByNameAndSource_shouldReturnPolicy() {
        Policy policy = createPolicy("name-source-test", PolicySeverity.MAJOR, PolicyStatus.ACTIVE);
        policyRepository.savePolicy(policy);

        Optional<Policy> found = policyRepository.findPolicyByNameAndSource("name-source-test", "test-source");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("name-source-test");
    }

    @Test
    void findAllPolicies_shouldReturnAll() {
        policyRepository.savePolicy(createPolicy("p1", PolicySeverity.MAJOR, PolicyStatus.ACTIVE));
        policyRepository.savePolicy(createPolicy("p2", PolicySeverity.CRITICAL, PolicyStatus.INACTIVE));

        List<Policy> all = policyRepository.findAllPolicies(null);
        assertThat(all).hasSize(2);
    }

    @Test
    void findAllPolicies_shouldFilterByStatus() {
        policyRepository.savePolicy(createPolicy("active-1", PolicySeverity.MAJOR, PolicyStatus.ACTIVE));
        policyRepository.savePolicy(createPolicy("inactive-1", PolicySeverity.MINOR, PolicyStatus.INACTIVE));

        List<Policy> active = policyRepository.findAllPolicies(PolicyStatus.ACTIVE);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getName()).isEqualTo("active-1");
    }

    @Test
    void findPoliciesBySource_shouldFilterBySource() {
        Policy p1 = createPolicy("src-a", PolicySeverity.MAJOR, PolicyStatus.ACTIVE);
        policyRepository.savePolicy(p1);

        List<Policy> fromSource = policyRepository.findPoliciesBySource("test-source");
        assertThat(fromSource).isNotEmpty();
        assertThat(fromSource.get(0).getSourceId()).isEqualTo("test-source");
    }

    @Test
    void updatePolicy_shouldUpdateFields() {
        Policy policy = createPolicy("to-update", PolicySeverity.MAJOR, PolicyStatus.ACTIVE);
        policy = policyRepository.savePolicy(policy);

        Policy updated = new Policy(
                policy.getId(),
                policy.getName(),
                "Updated description",
                PolicySeverity.CRITICAL,
                PolicyStatus.INACTIVE,
                policy.getScope(),
                "updated dsl",
                policy.getSourceId(),
                2,
                policy.getCreatedAt(),
                Instant.now());

        Policy result = policyRepository.updatePolicy(updated);
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
        assertThat(result.getVersion()).isEqualTo(2);
    }

    @Test
    void deletePolicy_shouldRemovePolicy() {
        Policy policy = createPolicy("to-delete", PolicySeverity.MAJOR, PolicyStatus.ACTIVE);
        policy = policyRepository.savePolicy(policy);

        policyRepository.deletePolicy(policy.getId());

        Optional<Policy> found = policyRepository.findPolicyById(policy.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void deletePoliciesBySource_shouldRemoveAllFromSource() {
        policyRepository.savePolicy(createPolicy("del-src-1", PolicySeverity.MAJOR, PolicyStatus.ACTIVE));
        policyRepository.savePolicy(createPolicy("del-src-2", PolicySeverity.MINOR, PolicyStatus.ACTIVE));

        int deleted = policyRepository.deletePoliciesBySource("test-source");
        assertThat(deleted).isEqualTo(2);

        List<Policy> remaining = policyRepository.findPoliciesBySource("test-source");
        assertThat(remaining).isEmpty();
    }

    // ---- PolicySet operations ----

    @Test
    void saveAndFindPolicySetById_shouldRoundTrip() {
        var policySet = new PolicySet(UUID.randomUUID(), "test-set", "Test set", List.of(), 1, now, now);

        PolicySet saved = policyRepository.savePolicySet(policySet);
        assertThat(saved.getId()).isEqualTo(policySet.getId());

        Optional<PolicySet> found = policyRepository.findPolicySetById(policySet.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-set");
    }

    @Test
    void findPolicySetByName_shouldReturnSet() {
        var policySet = new PolicySet(UUID.randomUUID(), "unique-set", "Unique", List.of(), 1, now, now);
        policyRepository.savePolicySet(policySet);

        Optional<PolicySet> found = policyRepository.findPolicySetByName("unique-set");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("unique-set");
    }

    @Test
    void findAllPolicySets_shouldReturnAll() {
        policyRepository.savePolicySet(new PolicySet(UUID.randomUUID(), "set-a", "", List.of(), 1, now, now));
        policyRepository.savePolicySet(new PolicySet(UUID.randomUUID(), "set-b", "", List.of(), 1, now, now));

        assertThat(policyRepository.findAllPolicySets()).hasSize(2);
    }

    // ---- Evaluation result operations ----

    @Test
    void saveAndFindEvaluationById_shouldRoundTrip() {
        var result = new PolicyEvaluationResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "org/repo",
                "openapi.yaml",
                "a".repeat(40),
                PolicyEvaluationResult.Verdict.PASS,
                List.of(),
                5,
                5,
                0,
                now);

        PolicyEvaluationResult saved = policyRepository.saveEvaluation(result);
        assertThat(saved.getId()).isEqualTo(result.getId());

        Optional<PolicyEvaluationResult> found = policyRepository.findEvaluationById(result.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
        assertThat(found.get().getTotalPoliciesChecked()).isEqualTo(5);
    }

    @Test
    void findEvaluationsBySpecId_shouldReturnOrdered() {
        UUID specId = UUID.randomUUID();
        var r1 = new PolicyEvaluationResult(
                UUID.randomUUID(),
                specId,
                UUID.randomUUID(),
                "org/repo",
                "spec.yaml",
                "a",
                PolicyEvaluationResult.Verdict.PASS,
                List.of(),
                1,
                1,
                0,
                now.minusSeconds(10));
        var r2 = new PolicyEvaluationResult(
                UUID.randomUUID(),
                specId,
                UUID.randomUUID(),
                "org/repo",
                "spec.yaml",
                "b",
                PolicyEvaluationResult.Verdict.FAIL,
                List.of(),
                1,
                0,
                1,
                now);

        policyRepository.saveEvaluation(r1);
        policyRepository.saveEvaluation(r2);

        List<PolicyEvaluationResult> results = policyRepository.findEvaluationsBySpecId(specId, 10);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getEvaluatedAt())
                .isAfterOrEqualTo(results.get(1).getEvaluatedAt());
    }

    // ---- Edge cases ----

    @Test
    void saveAndFindPolicy_shouldHandleScopeWithPatterns() {
        var scope = new PolicyScope(
                java.util.Set.of("/api/v1/**", "/api/v2/**"),
                java.util.Set.of(PolicyScope.HttpOperation.GET, PolicyScope.HttpOperation.POST),
                java.util.Set.of("core", "payment"),
                java.util.Set.of("/health", "/metrics"));

        Policy policy = new Policy(
                UUID.randomUUID(),
                "scoped-policy",
                null,
                PolicySeverity.MAJOR,
                PolicyStatus.ACTIVE,
                scope,
                "each endpoint in spec.endpoints yield pass()",
                "test-source",
                1,
                now,
                now);

        policyRepository.savePolicy(policy);

        Optional<Policy> found = policyRepository.findPolicyById(policy.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getScope().pathPatterns()).contains("/api/v1/**", "/api/v2/**");
        assertThat(found.get().getScope().operations())
                .contains(PolicyScope.HttpOperation.GET, PolicyScope.HttpOperation.POST);
        assertThat(found.get().getScope().excludePaths()).contains("/health");
    }

    @Test
    void deactivateStalePolicies_shouldDeactivateUnlisted() {
        policyRepository.savePolicy(createPolicy("keep", PolicySeverity.MAJOR, PolicyStatus.ACTIVE));
        policyRepository.savePolicy(createPolicy("stale", PolicySeverity.MINOR, PolicyStatus.ACTIVE));

        int deactivated = policyRepository.deactivateStalePolicies(List.of("keep"));
        assertThat(deactivated).isGreaterThanOrEqualTo(1);

        // The "stale" policy should now be INACTIVE
        List<Policy> active = policyRepository.findAllPolicies(PolicyStatus.ACTIVE);
        assertThat(active.stream().map(Policy::getName)).contains("keep").doesNotContain("stale");

        List<Policy> inactive = policyRepository.findAllPolicies(PolicyStatus.INACTIVE);
        assertThat(inactive.stream().map(Policy::getName)).contains("stale");
    }
}
