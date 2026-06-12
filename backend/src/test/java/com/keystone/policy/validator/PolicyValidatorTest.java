package com.keystone.policy.validator;

import com.keystone.policy.domain.exception.PolicyParseException;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyScope;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.domain.model.PolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyValidatorTest {

    private PolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PolicyValidator();
    }

    @Test
    void validateAndParse_shouldReturnValidPolicy() {
        Policy policy = validator.validateAndParse(
                "test-policy", "A test policy", "MAJOR", "ACTIVE",
                "each endpoint in spec.endpoints where not endpoint.has(\"operationId\") yield violation(\"Missing operationId\")",
                "test-source", PolicyScope.all());

        assertThat(policy).isNotNull();
        assertThat(policy.getName()).isEqualTo("test-policy");
        assertThat(policy.getSeverity()).isEqualTo(PolicySeverity.MAJOR);
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(policy.getSourceId()).isEqualTo("test-source");
        assertThat(policy.getVersion()).isEqualTo(1);
    }

    @Test
    void validateAndParse_shouldUseDefaultsForMissingOptionalFields() {
        Policy policy = validator.validateAndParse(
                "minimal-policy", null, null, null,
                "each endpoint in spec.endpoints yield pass()",
                "default", PolicyScope.all());

        assertThat(policy).isNotNull();
        assertThat(policy.getSeverity()).isEqualTo(PolicySeverity.MAJOR);
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
    }

    @Test
    void validateAndParse_shouldRejectInvalidName() {
        assertThatThrownBy(() -> validator.validateAndParse(
                "Invalid Name With Spaces", null, "MAJOR", "ACTIVE",
                "each endpoint in spec.endpoints yield pass()",
                "test", PolicyScope.all()))
                .isInstanceOf(PolicyParseException.class);
    }

    @Test
    void validateAndParse_shouldRejectInvalidSeverity() {
        assertThatThrownBy(() -> validator.validateAndParse(
                "valid-name", null, "INVALID_SEVERITY", "ACTIVE",
                "each endpoint in spec.endpoints yield pass()",
                "test", PolicyScope.all()))
                .isInstanceOf(PolicyParseException.class);
    }

    @Test
    void validateDslSyntax_shouldAcceptValidDsl() {
        var errors = validator.validateDslSyntax(
                "each endpoint in spec.endpoints where endpoint.is_deprecated yield violation(\"Deprecated\")");
        assertThat(errors).isEmpty();
    }

    @Test
    void validateDslSyntax_shouldAcceptAnyQuantifier() {
        assertThat(validator.validateDslSyntax(
                "any endpoint in spec.paths where not endpoint.has(\"operationId\") yield violation(\"Missing\")"))
                .isEmpty();

        assertThat(validator.validateDslSyntax(
                "none field in spec.schemas where field.is_deprecated yield violation(\"Deprecated\")"))
                .isEmpty();
    }

    @Test
    void validateDslSyntax_shouldRejectMissingQuantifier() {
        var errors = validator.validateDslSyntax(
                "endpoint in spec.endpoints yield violation(\"Test\")");
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0).message()).contains("quantifier");
    }

    @Test
    void validateDslSyntax_shouldRejectMissingYield() {
        var errors = validator.validateDslSyntax(
                "each endpoint in spec.endpoints do something");
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0).message()).contains("yield");
    }

    @Test
    void validateDslSyntax_shouldRejectMissingInClause() {
        var errors = validator.validateDslSyntax(
                "each endpoint.something yield pass()");
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0).message()).contains("in <source>");
    }

    @Test
    void isValidDsl_shouldReturnTrueForValidExpression() {
        assertThat(validator.isValidDsl(
                "each endpoint in spec.endpoints yield pass()"))
                .isTrue();
    }

    @Test
    void isValidDsl_shouldReturnFalseForInvalidExpression() {
        assertThat(validator.isValidDsl("invalid expression")).isFalse();
    }

    @Test
    void validateAndParse_shouldRejectEmptyDsl() {
        assertThatThrownBy(() -> validator.validateAndParse(
                "empty-rule", null, "MAJOR", "ACTIVE",
                "", "test", PolicyScope.all()))
                .isInstanceOf(PolicyParseException.class);
    }
}
