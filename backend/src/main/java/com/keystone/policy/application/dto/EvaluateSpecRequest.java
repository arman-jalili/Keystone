// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;

/**
 * Input DTO for requesting policy evaluation against a specification.
 *
 * @param repository  The repository identifier in "owner/name" format (e.g. "org/repo")
 * @param specPath    The relative path to the spec file within the repository
 * @param commitSha   The full git commit SHA (40 hex characters)
 * @param policySetId Optional policy set ID. If omitted, all active policy sets are evaluated.
 */
public record EvaluateSpecRequest(
        @NotBlank(message = "repository is required")
                @Size(max = 256, message = "repository must not exceed 256 characters")
                String repository,
        @NotBlank(message = "specPath is required")
                @Size(max = 512, message = "specPath must not exceed 512 characters")
                String specPath,
        @NotBlank(message = "commitSha is required")
                @Size(min = 40, max = 40, message = "commitSha must be exactly 40 hex characters")
                String commitSha,
        UUID policySetId) {
    public EvaluateSpecRequest {
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
    }

    public boolean hasSpecificPolicySet() {
        return policySetId != null;
    }
}
