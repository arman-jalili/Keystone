// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.application.dto;

import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.domain.model.PolicyStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Output DTO for a summary view of a policy (list view).
 *
 * @param id          The UUID of the policy
 * @param name        The unique name of the policy
 * @param description A short description of the policy
 * @param severity    The severity level if violated
 * @param status      The current lifecycle status
 * @param version     The current version number
 * @param sourceId    The identifier of the source the policy was loaded from
 * @param createdAt   When the policy was created
 * @param updatedAt   When the policy was last updated
 */
public record PolicySummaryResponse(
        UUID id,
        String name,
        String description,
        PolicySeverity severity,
        PolicyStatus status,
        int version,
        String sourceId,
        Instant createdAt,
        Instant updatedAt) {
    public PolicySummaryResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Creates a {@link PolicySummaryResponse} from a domain {@link Policy}.
     */
    public static PolicySummaryResponse from(Policy policy) {
        return new PolicySummaryResponse(
                policy.getId(),
                policy.getName(),
                policy.getDescription(),
                policy.getSeverity(),
                policy.getStatus(),
                policy.getVersion(),
                policy.getSourceId(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }
}
