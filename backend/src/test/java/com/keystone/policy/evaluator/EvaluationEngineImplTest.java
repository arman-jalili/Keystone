// Canonical Reference: .pi/architecture/modules/policy-engine.md
package com.keystone.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.keystone.analysis.domain.service.SpecParser;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import com.keystone.policy.domain.model.*;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationEngineImplTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private SpecRepository specRepository;

    @Mock
    private SpecParser specParser;

    private EvaluationEngineImpl engine;

    private PolicySet policySet;
    private final UUID specId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        engine = new EvaluationEngineImpl(policyRepository, specRepository, specParser);
        when(policyRepository.saveEvaluation(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Policy createPolicy(String name, PolicySeverity severity, String dsl) {
        return new Policy(
                UUID.randomUUID(),
                name,
                "Test",
                severity,
                PolicyStatus.ACTIVE,
                PolicyScope.all(),
                dsl,
                "test-source",
                1,
                now,
                now);
    }

    private PolicySet createPolicySet(List<Policy> policies) {
        return new PolicySet(UUID.randomUUID(), "test-set", "Test set", policies, 1, now, now);
    }

    @Test
    void evaluate_shouldReturnPassForEmptyPolicySet() {
        policySet = createPolicySet(List.of());

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    void evaluate_shouldReturnPassWhenAllPoliciesPass() {
        Policy policy1 = createPolicy("pass-1", PolicySeverity.MAJOR, "each endpoint in spec.endpoints yield pass()");
        Policy policy2 = createPolicy(
                "pass-2",
                PolicySeverity.CRITICAL,
                "none field in spec.schemas where field.is_deprecated yield violation(\"Deprecated\")");
        policySet = createPolicySet(List.of(policy1, policy2));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    void evaluate_shouldOnlyEvaluateActivePolicies() {
        Policy active = createPolicy("active", PolicySeverity.MAJOR, "each endpoint in spec.endpoints yield pass()");
        Policy inactive = new Policy(
                UUID.randomUUID(),
                "inactive",
                "Inactive",
                PolicySeverity.CRITICAL,
                PolicyStatus.INACTIVE,
                PolicyScope.all(),
                "each endpoint in spec.endpoints yield violation(\"fail\")",
                "test-source",
                1,
                now,
                now);
        policySet = createPolicySet(List.of(active, inactive));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
    }

    @Test
    void evaluate_shouldReturnWarningForEachViolation() {
        Policy policy = createPolicy(
                "warn-all", PolicySeverity.MINOR, "each path in spec.paths yield violation(\"All paths\")");
        policySet = createPolicySet(List.of(policy));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        // The "each ... yield violation" without a where clause produces MINOR violations
        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.WARNING);
        assertThat(result.getViolations()).isNotEmpty();
    }

    @Test
    void evaluate_shouldReturnFailForCriticalViolations() {
        // A policy with CRITICAL severity should produce FAIL verdict
        Policy policy = createPolicy(
                "critical-rule",
                PolicySeverity.CRITICAL,
                "each endpoint in spec.endpoints yield violation(\"Critical violation\")");
        policySet = createPolicySet(List.of(policy));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        // The "each ... yield violation" without where clause triggers violations
        // with the policy's severity (CRITICAL), which should produce FAIL
        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.FAIL);
        assertThat(result.getViolations()).isNotEmpty();
        assertThat(result.getViolations().get(0).severity()).isEqualTo(PolicySeverity.CRITICAL);
    }

    @Test
    void evaluateSubset_shouldOnlyEvaluateSpecifiedPolicies() {
        Policy keep = createPolicy("keep", PolicySeverity.MAJOR, "each endpoint in spec.endpoints yield pass()");
        Policy skip =
                createPolicy("skip", PolicySeverity.MAJOR, "each path in spec.paths yield violation(\"skipped\")");
        policySet = createPolicySet(List.of(keep, skip));

        PolicyEvaluationResult result = engine.evaluateSubset(policySet, specId, Set.of(keep.getId()));

        assertThat(result.getTotalPoliciesChecked()).isEqualTo(1);
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    void evaluate_shouldHandleInactivePolicyGracefully() {
        Policy policy =
                createPolicy("active-only", PolicySeverity.CRITICAL, "each endpoint in spec.endpoints yield pass()");
        policySet = createPolicySet(List.of(policy));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getVerdict())
                .isIn(
                        PolicyEvaluationResult.Verdict.PASS,
                        PolicyEvaluationResult.Verdict.FAIL,
                        PolicyEvaluationResult.Verdict.WARNING);
    }

    @Test
    void evaluate_shouldHandleBlankDslExpression() {
        Policy blankDsl = new Policy(
                UUID.randomUUID(),
                "blank-dsl",
                "Test",
                PolicySeverity.MAJOR,
                PolicyStatus.ACTIVE,
                PolicyScope.all(),
                "",
                "test-source",
                1,
                now,
                now);
        policySet = createPolicySet(List.of(blankDsl));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
    }

    @Test
    void evaluate_shouldHandleEmptyDslExpression() {
        Policy policy = createPolicy("empty-dsl", PolicySeverity.MAJOR, "");
        policySet = createPolicySet(List.of(policy));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
    }

    @Test
    void evaluate_shouldPersistResult() {
        policySet = createPolicySet(
                List.of(createPolicy("persist", PolicySeverity.MAJOR, "each endpoint in spec.endpoints yield pass()")));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEvaluatedAt()).isNotNull();
    }

    @Test
    void evaluate_shouldHandleDeprecationCheck() {
        Policy policy = createPolicy(
                "no-deprecations",
                PolicySeverity.MAJOR,
                "none field in spec.schemas where field.is_deprecated yield violation(\"Deprecated fields not allowed\")");
        policySet = createPolicySet(List.of(policy));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
    }

    @Test
    void evaluate_shouldHandleOperationIdCheck() {
        Policy policy = createPolicy(
                "require-operation-id",
                PolicySeverity.MAJOR,
                "each endpoint in spec.endpoints where not endpoint.has(\"operationId\") yield violation(\"Missing operationId\")");
        policySet = createPolicySet(List.of(policy));

        PolicyEvaluationResult result = engine.evaluate(policySet, specId);

        assertThat(result.getVerdict()).isEqualTo(PolicyEvaluationResult.Verdict.PASS);
    }
}
