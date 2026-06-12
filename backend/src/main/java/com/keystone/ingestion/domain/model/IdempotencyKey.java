package com.keystone.ingestion.domain.model;

import java.util.Objects;

/**
 * Value object representing the idempotency key used for deduplication.
 *
 * <p>Per ADR-007, the composite key is {@code (repository, commitSha, specPath)}.
 * This ensures the same spec from the same commit is not processed more than once,
 * even when the same event arrives via multiple paths (e.g. webhook + CLI upload).
 *
 * @param repository  The repository identifier (e.g. "org/repo")
 * @param commitSha   The full git commit SHA
 * @param specPath    The relative path to the spec file within the repository
 */
public record IdempotencyKey(
    String repository,
    String commitSha,
    String specPath
) {
    public IdempotencyKey {
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        if (repository.isBlank()) throw new IllegalArgumentException("repository must not be blank");
        if (commitSha.isBlank()) throw new IllegalArgumentException("commitSha must not be blank");
        if (specPath.isBlank()) throw new IllegalArgumentException("specPath must not be blank");
    }
}
