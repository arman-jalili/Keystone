package com.keystone.breaking.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Input DTO for requesting a breaking change analysis.
 *
 * <p>Sent by the Keystone CLI (audit upload) or triggered automatically
 * after a spec is ingested. The {@code repository}, {@code specPath},
 * and {@code commitSha} identify which spec version to analyse.
 *
 * @param repository  The repository identifier in "owner/name" format (e.g. "org/repo")
 * @param specPath    The relative path to the spec file within the repository
 * @param commitSha   The full git commit SHA (40 hex characters) of the version to analyse
 * @param baseVersion Optional explicit base version override (e.g. "v1.2.3" or commit SHA).
 *                    If omitted, the system resolves the base version automatically.
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

    @Size(max = 256, message = "baseVersion must not exceed 256 characters")
    String baseVersion
) {
    public AnalysisRequest {
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
    }

    /**
     * Whether the caller explicitly provided a base version.
     */
    public boolean hasExplicitBaseVersion() {
        return baseVersion != null && !baseVersion.isBlank();
    }
}
