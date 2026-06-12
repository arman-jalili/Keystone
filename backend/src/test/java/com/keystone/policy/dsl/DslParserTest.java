package com.keystone.policy.dsl;

import com.keystone.policy.domain.exception.PolicyParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DslParserTest {

    private DslParser parser;

    @BeforeEach
    void setUp() {
        parser = new DslParser();
    }

    @Test
    void parse_shouldHandleSimpleEachPass() {
        DslExpression expr = parser.parse("each endpoint in spec.endpoints yield pass()");

        assertThat(expr.quantifier()).isEqualTo(DslExpression.Quantifier.EACH);
        assertThat(expr.source()).isEqualTo("spec.endpoints");
        assertThat(expr.condition()).isNull();
        assertThat(expr.action()).isEqualTo(DslExpression.Action.PASS);
    }

    @Test
    void parse_shouldHandleAnyPass() {
        DslExpression expr = parser.parse("any path in spec.paths yield pass()");

        assertThat(expr.quantifier()).isEqualTo(DslExpression.Quantifier.ANY);
        assertThat(expr.source()).isEqualTo("spec.paths");
        assertThat(expr.action()).isEqualTo(DslExpression.Action.PASS);
    }

    @Test
    void parse_shouldHandleNoneWithCondition() {
        DslExpression expr = parser.parse(
                "none field in spec.schemas where field.is_deprecated yield violation(\"Deprecated\")");

        assertThat(expr.quantifier()).isEqualTo(DslExpression.Quantifier.NONE);
        assertThat(expr.source()).isEqualTo("spec.schemas");
        assertThat(expr.condition()).isNotNull();
        assertThat(expr.action()).isEqualTo(DslExpression.Action.VIOLATION);
        assertThat(expr.actionArg()).isEqualTo("Deprecated");
    }

    @Test
    void parse_shouldHandleViolationAction() {
        DslExpression expr = parser.parse(
                "each endpoint in spec.endpoints yield violation(\"Must have operationId\")");

        assertThat(expr.action()).isEqualTo(DslExpression.Action.VIOLATION);
        assertThat(expr.actionArg()).isEqualTo("Must have operationId");
    }

    @Test
    void parse_shouldHandleConditionWithComparison() {
        DslExpression expr = parser.parse(
                "each endpoint in spec.endpoints where endpoint.protocol != \"https\" yield violation(\"Must use HTTPS\")");

        assertThat(expr.condition()).isInstanceOf(DslExpression.ComparisonCondition.class);
    }

    @Test
    void parse_shouldHandleNotCondition() {
        DslExpression expr = parser.parse(
                "each endpoint in spec.endpoints where not endpoint.has(\"operationId\") yield violation(\"Missing\")");

        assertThat(expr.condition()).isNotNull();
    }

    @Test
    void parse_shouldThrowForEmptyExpression() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(PolicyParseException.class);
    }

    @Test
    void parse_shouldThrowForNullExpression() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(PolicyParseException.class);
    }

    @Test
    void parse_shouldThrowForMissingYield() {
        assertThatThrownBy(() -> parser.parse("each endpoint in spec.endpoints do something"))
                .isInstanceOf(PolicyParseException.class);
    }

    @Test
    void parse_shouldThrowForNoQuantifier() {
        assertThatThrownBy(() -> parser.parse("endpoint in spec.endpoints yield pass()"))
                .isInstanceOf(PolicyParseException.class);
    }

    @Test
    void tokenize_shouldSplitSimpleInput() {
        List<String> tokens = parser.tokenize("each endpoint in spec.paths yield pass()");
        assertThat(tokens).containsExactly("each", "endpoint", "in", "spec.paths", "yield", "pass()");
    }

    @Test
    void tokenize_shouldHandleQuotedStrings() {
        List<String> tokens = parser.tokenize(
                "yield violation(\"Missing operationId\")");
        assertThat(tokens).containsExactly("yield", "violation(\"Missing operationId\")");
    }

    @Test
    void parse_shouldHandleDeprecationCheck() {
        DslExpression expr = parser.parse(
                "none field in spec.schemas where field.is_deprecated yield violation(\"No deprecated fields\")");

        assertThat(expr.quantifier()).isEqualTo(DslExpression.Quantifier.NONE);
        assertThat(expr.action()).isEqualTo(DslExpression.Action.VIOLATION);
    }

    @Test
    void parse_shouldHandlePathPatternCheck() {
        DslExpression expr = parser.parse(
                "each path in spec.paths where path.matches(\"^/api/v[0-9]+/\") yield pass()");

        assertThat(expr.source()).isEqualTo("spec.paths");
        assertThat(expr.action()).isEqualTo(DslExpression.Action.PASS);
    }

    @Test
    void parse_shouldExtractActionArgWithQuotes() {
        DslExpression expr = parser.parse(
                "each endpoint in spec.endpoints yield violation(\"Must be HTTPS\")");
        assertThat(expr.actionArg()).isEqualTo("Must be HTTPS");
    }

    @Test
    void parse_shouldUseDefaultActionForUnknownAction() {
        assertThatThrownBy(() -> parser.parse(
                "each endpoint in spec.endpoints yield unknown()"))
                .isInstanceOf(PolicyParseException.class);
    }
}
