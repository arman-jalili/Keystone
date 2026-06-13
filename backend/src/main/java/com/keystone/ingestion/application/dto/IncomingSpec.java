// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Input DTO for ingesting an OpenAPI specification.
 *
 * <p>Sent by the Keystone CLI (audit upload) or extracted from webhook payloads.
 * The {@code repository}, {@code commitSha}, and {@code specPath} together
 * form the idempotency key per ADR-007.
 *
 * @param repository  The repository identifier in "owner/name" format (e.g. "org/repo")
 * @param commitSha   The full git commit SHA (40 hex characters)
 * @param specPath    The relative path to the spec file within the repository
 * @param content     The raw OpenAPI 3.x specification content (YAML or JSON)
 */
public record IncomingSpec(
        @NotBlank(message = "repository is required")
                @Size(max = 256, message = "repository must not exceed 256 characters")
                String repository,
        @NotBlank(message = "commitSha is required")
                @Size(min = 40, max = 40, message = "commitSha must be exactly 40 hex characters")
                String commitSha,
        @NotBlank(message = "specPath is required")
                @Size(max = 512, message = "specPath must not exceed 512 characters")
                String specPath,
        @NotBlank(message = "content is required") @Size(max = 10_485_760, message = "content must not exceed 10 MB")
                String content) {
    public IncomingSpec {
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
