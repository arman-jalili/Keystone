package com.keystone.policy.application.dto;

import com.keystone.policy.domain.model.PolicyScope;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.domain.model.PolicyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Input DTO for creating a new policy rule.
 *
 * @param name          A unique human-readable name for the policy (e.g. "require-https")
 * @param description   Optional description explaining the policy's purpose
 * @param severity      The severity level if this policy is violated
 * @param status        The initial lifecycle status (usually ACTIVE)
 * @param scope         The target scope defining which spec elements the policy applies to
 * @param dslExpression The Policy DSL expression defining the rule logic
 */
public record CreatePolicyRequest(
        @NotBlank(message = "name is required")
                @Size(min = 2, max = 128, message = "name must be between 2 and 128 characters")
                @Pattern(
                        regexp = "^[a-z0-9]([a-z0-9_-]*[a-z0-9])?$",
                        message = "name must be lowercase alphanumeric with optional hyphens/underscores")
                String name,
        @Size(max = 1024, message = "description must not exceed 1024 characters") String description,
        @NotNull(message = "severity is required") PolicySeverity severity,
        @NotNull(message = "status is required") PolicyStatus status,
        @NotNull(message = "scope is required") PolicyScope scope,
        @NotBlank(message = "dslExpression is required")
                @Size(max = 65535, message = "dslExpression must not exceed 65535 characters")
                String dslExpression) {
    public CreatePolicyRequest {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(dslExpression, "dslExpression must not be null");
    }
}
