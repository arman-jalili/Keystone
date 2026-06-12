package com.keystone.policy.application.dto;

import com.keystone.policy.domain.model.PolicyScope;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.domain.model.PolicyStatus;
import jakarta.validation.constraints.Size;

/**
 * Input DTO for updating an existing policy rule.
 *
 * <p>All fields are optional — only provided fields will be updated.
 * Null fields are ignored during the update operation.
 *
 * @param description   Updated description (null = unchanged)
 * @param severity      Updated severity (null = unchanged)
 * @param status        Updated lifecycle status (null = unchanged)
 * @param scope         Updated target scope (null = unchanged)
 * @param dslExpression Updated DSL expression (null = unchanged)
 */
public record UpdatePolicyRequest(
        @Size(max = 1024, message = "description must not exceed 1024 characters") String description,
        PolicySeverity severity,
        PolicyStatus status,
        PolicyScope scope,
        @Size(max = 65535, message = "dslExpression must not exceed 65535 characters") String dslExpression) {
    public boolean hasUpdates() {
        return description != null || severity != null || status != null || scope != null || dslExpression != null;
    }
}
