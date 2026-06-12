package com.keystone.policy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Input DTO for triggering a policy synchronization from an external source.
 *
 * @param sourceId The identifier of the policy source to sync from
 * @param ref      Optional git ref/branch to sync from (default: source configuration default)
 */
public record SyncPoliciesRequest(
        @NotBlank(message = "sourceId is required")
                @Size(max = 256, message = "sourceId must not exceed 256 characters")
                String sourceId,
        @Size(max = 256, message = "ref must not exceed 256 characters") String ref) {
    public SyncPoliciesRequest {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
    }

    public boolean hasExplicitRef() {
        return ref != null && !ref.isBlank();
    }
}
