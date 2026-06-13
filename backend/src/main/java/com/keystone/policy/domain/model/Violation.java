// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.domain.model;

import java.util.Objects;

/**
 * A single policy violation detected during evaluation.
 *
 * <p>Represents one finding where an OpenAPI specification element
 * violates a policy rule. Multiple violations may be produced for
 * a single policy if it matches multiple spec elements.
 *
 * @param policyId        The UUID of the violated policy
 * @param policyName      The name of the violated policy
 * @param severity        The severity of this violation
 * @param message         A human-readable description of the violation
 * @param specPath        JSON Pointer path within the spec where the violation occurred (e.g. "/paths/~1api~1v1~1users/get/responses/200")
 * @param suggestedFix    Optional suggestion for how to fix the violation
 */
public record Violation(
        java.util.UUID policyId,
        String policyName,
        PolicySeverity severity,
        String message,
        String specPath,
        String suggestedFix) {
    public Violation {
        Objects.requireNonNull(policyId, "policyId must not be null");
        Objects.requireNonNull(policyName, "policyName must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
    }
}
