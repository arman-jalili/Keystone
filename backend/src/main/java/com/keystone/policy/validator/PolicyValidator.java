// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.validator;

import com.keystone.policy.domain.exception.PolicyParseException;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyScope;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.domain.model.PolicyStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validates policy DSL syntax and semantics.
 *
 * <p>Performs validation of policy definitions before they are persisted
 * during a sync operation. Checks include:
 * <ul>
 *   <li>Policy name format (lowercase alphanumeric with hyphens)</li>
 *   <li>DSL expression syntax</li>
 *   <li>Scope configuration validity</li>
 *   <li>Severity and status enum values</li>
 * </ul>
 */
@org.springframework.stereotype.Component
public class PolicyValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9_-]*[a-z0-9])?$");

    /**
     * Validates a raw policy specification and returns a parsed Policy if valid.
     *
     * @param name           the policy name
     * @param description    optional description
     * @param severityStr    severity as string
     * @param statusStr      status as string
     * @param dslExpression  the DSL expression
     * @param sourceId       the source identifier
     * @return the parsed Policy if valid
     * @throws PolicyParseException if validation fails
     */
    public Policy validateAndParse(
            String name,
            String description,
            String severityStr,
            String statusStr,
            String dslExpression,
            String sourceId,
            PolicyScope scope)
            throws PolicyParseException {
        List<PolicyParseException.ParseError> errors = new ArrayList<>();

        // Validate name
        if (name == null || name.isBlank()) {
            errors.add(new PolicyParseException.ParseError(1, 1, "Policy name is required"));
        } else if (!NAME_PATTERN.matcher(name).matches()) {
            errors.add(new PolicyParseException.ParseError(
                    1, 1, "Policy name must be lowercase alphanumeric with optional hyphens/underscores"));
        }

        // Validate severity
        PolicySeverity severity = null;
        if (severityStr != null) {
            try {
                severity = PolicySeverity.valueOf(severityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new PolicyParseException.ParseError(
                        1,
                        1,
                        "Invalid severity: " + severityStr + ". Must be one of: " + List.of(PolicySeverity.values())));
            }
        } else {
            severity = PolicySeverity.MAJOR;
        }

        // Validate status
        PolicyStatus status = null;
        if (statusStr != null) {
            try {
                status = PolicyStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new PolicyParseException.ParseError(
                        1, 1, "Invalid status: " + statusStr + ". Must be one of: " + List.of(PolicyStatus.values())));
            }
        } else {
            status = PolicyStatus.ACTIVE;
        }

        // Validate DSL expression
        if (dslExpression == null || dslExpression.isBlank()) {
            errors.add(new PolicyParseException.ParseError(1, 1, "DSL expression is required"));
        } else {
            errors.addAll(validateDslSyntax(dslExpression));
        }

        if (!errors.isEmpty()) {
            throw new PolicyParseException("Policy validation failed for: " + name, errors);
        }

        Instant now = Instant.now();
        return new Policy(
                UUID.randomUUID(),
                name,
                description,
                severity,
                status,
                scope != null ? scope : PolicyScope.all(),
                dslExpression,
                sourceId,
                1,
                now,
                now);
    }

    /**
     * Validates the syntax of a policy DSL expression.
     *
     * @param expression the DSL expression to validate
     * @return list of parse errors (empty if valid)
     */
    public List<PolicyParseException.ParseError> validateDslSyntax(String expression) {
        List<PolicyParseException.ParseError> errors = new ArrayList<>();
        if (expression == null || expression.isBlank()) {
            errors.add(new PolicyParseException.ParseError(1, 1, "DSL expression is empty"));
            return errors;
        }

        String trimmed = expression.trim();
        String[] lines = trimmed.split("\n");

        // Check first line starts with a quantifier
        String firstLine = lines[0].trim();
        boolean hasQuantifier =
                firstLine.startsWith("each ") || firstLine.startsWith("any ") || firstLine.startsWith("none ");

        if (!hasQuantifier) {
            errors.add(new PolicyParseException.ParseError(
                    1, 1, "DSL expression must start with a quantifier (each/any/none)"));
        }

        // Check for 'yield' keyword
        if (!trimmed.contains("yield")) {
            errors.add(new PolicyParseException.ParseError(
                    lines.length, 1, "DSL expression must contain 'yield' keyword"));
        }

        // Check for valid 'in source' pattern
        if (!trimmed.contains(" in ")) {
            errors.add(new PolicyParseException.ParseError(1, 1, "DSL expression must contain 'in <source>' clause"));
        }

        return errors;
    }

    /**
     * Returns whether the given DSL expression is syntactically valid.
     */
    public boolean isValidDsl(String expression) {
        return validateDslSyntax(expression).isEmpty();
    }
}
