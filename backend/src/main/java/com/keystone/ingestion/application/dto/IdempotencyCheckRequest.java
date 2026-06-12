package com.keystone.ingestion.application.dto;

import com.keystone.ingestion.domain.model.IdempotencyKey;

import java.util.Objects;

/**
 * Request payload for checking idempotency before upload.
 *
 * <p>Optional pre-flight check that lets the CLI verify whether a spec
 * has already been ingested without sending the full content.
 *
 * @param repository  The repository identifier (e.g. "org/repo")
 * @param commitSha   The full git commit SHA
 * @param specPath    The relative path to the spec file
 */
public record IdempotencyCheckRequest(
    String repository,
    String commitSha,
    String specPath
) {
    public IdempotencyCheckRequest {
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
    }

    /**
     * Converts this request into a domain {@link IdempotencyKey}.
     */
    public IdempotencyKey toDomainKey() {
        return new IdempotencyKey(repository, commitSha, specPath);
    }
}
