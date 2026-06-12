package com.keystone.policy.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.policy.domain.model.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DslExecutorTest {

    private DslExecutor executor;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        executor = new DslExecutor(new DslParser());
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

    @Test
    void evaluate_shouldReturnNoViolationsForPassAction() {
        Policy policy = createPolicy("all-pass", PolicySeverity.MAJOR, "each endpoint in spec.endpoints yield pass()");

        List<Violation> violations = executor.evaluate(policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void evaluate_shouldReturnViolationForEachWithViolation() {
        Policy policy = createPolicy(
                "all-violate", PolicySeverity.MAJOR, "each endpoint in spec.endpoints yield violation(\"All fail\")");

        List<Violation> violations = executor.evaluate(policy);

        // Should have violations for each endpoint
        assertThat(violations).isNotEmpty();
    }

    @Test
    void evaluate_shouldDeprecatedCheckPass() {
        Policy policy = createPolicy(
                "no-deprecations",
                PolicySeverity.MAJOR,
                "none field in spec.schemas where field.is_deprecated yield violation(\"Deprecated\")");

        List<Violation> violations = executor.evaluate(policy);

        // Product schema is deprecated, so "none" should yield violations
        assertThat(violations).isNotEmpty();
    }

    @Test
    void evaluate_shouldHandleOperationIdCheck() {
        Policy policy = createPolicy(
                "require-operation-id",
                PolicySeverity.MAJOR,
                "each endpoint in spec.endpoints where not endpoint.has(\"operationId\") yield violation(\"Missing\")");

        List<Violation> violations = executor.evaluate(policy);

        // All mock endpoints have operationId, so no violations
        assertThat(violations).isEmpty();
    }

    @Test
    void evaluate_shouldHandleUnconditionalViolation() {
        Policy policy = createPolicy(
                "always-fail",
                PolicySeverity.CRITICAL,
                "each endpoint in spec.endpoints yield violation(\"Always fails\")");

        List<Violation> violations = executor.evaluate(policy);

        assertThat(violations).isNotEmpty();
        assertThat(violations.get(0).severity()).isEqualTo(PolicySeverity.CRITICAL);
    }

    @Test
    void evaluate_shouldHandleAnyQuantifier() {
        Policy policy = createPolicy(
                "any-match",
                PolicySeverity.MAJOR,
                "any endpoint in spec.operations where endpoint.method == \"DELETE\" yield violation(\"Delete not allowed\")");

        List<Violation> violations = executor.evaluate(policy);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void evaluate_shouldReturnEmptyForMalformedDsl() {
        Policy policy = createPolicy("bad-dsl", PolicySeverity.MAJOR, "not valid dsl at all");

        List<Violation> violations = executor.evaluate(policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void evaluate_shouldHandleEmptyDsl() {
        Policy policy = createPolicy("empty-dsl", PolicySeverity.MAJOR, "");

        List<Violation> violations = executor.evaluate(policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void evaluate_shouldHandleComparisonCondition() {
        Policy policy = createPolicy(
                "https-only",
                PolicySeverity.CRITICAL,
                "each endpoint in spec.endpoints where endpoint.protocol != \"https\" yield violation(\"HTTPS required\")");

        List<Violation> violations = executor.evaluate(policy);

        // All mock endpoints use https, so no violations
        assertThat(violations).isEmpty();
    }

    @Test
    void evaluate_shouldHandleSchemaScope() {
        Policy policy = createPolicy("schema-check", PolicySeverity.MINOR, "each field in spec.schemas yield pass()");

        List<Violation> violations = executor.evaluate(policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void evaluate_shouldHandleMessageInterpolation() {
        Policy policy = createPolicy(
                "interpolated",
                PolicySeverity.MAJOR,
                "each endpoint in spec.operations where endpoint.method == \"DELETE\" yield violation(\"Method {endpoint.method} not allowed on {endpoint.path}\")");

        List<Violation> violations = executor.evaluate(policy);

        assertThat(violations).isNotEmpty();
        assertThat(violations.get(0).message()).contains("DELETE");
    }
}
