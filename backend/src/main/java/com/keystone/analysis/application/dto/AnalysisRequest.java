package com.keystone.analysis.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Input DTO for requesting a breaking change analysis.
 *
 * @param repository      The repository identifier in "owner/name" format
 * @param specPath        The relative path to the spec file within the repository
 * @param commitSha       The full git commit SHA of the version to analyse
 * @param explicitBaseRef Optional explicit base reference (commit SHA or version tag).
 *                        If omitted, the system resolves the base version automatically.
 */
public record AnalysisRequest(
        @NotBlank(message = "repository is required")
                @Size(max = 256, message = "repository must not exceed 256 characters")
                String repository,
        @NotBlank(message = "specPath is required")
                @Size(max = 512, message = "specPath must not exceed 512 characters")
                String specPath,
        @NotBlank(message = "commitSha is required")
                @Size(min = 40, max = 40, message = "commitSha must be exactly 40 hex characters")
                String commitSha,
        @Size(max = 256, message = "explicitBaseRef must not exceed 256 characters") String explicitBaseRef) {
    public AnalysisRequest {
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
    }

    public boolean hasExplicitBaseRef() {
        return explicitBaseRef != null && !explicitBaseRef.isBlank();
    }
}
