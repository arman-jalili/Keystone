package com.keystone.breaking.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing a resolved base version for comparison.
 *
 * <p>Produced by {@link com.keystone.breaking.domain.service.BaseVersionResolver}
 * and consumed by {@link com.keystone.breaking.domain.service.DiffOrchestrator}
 * to determine which specification version the incoming spec should be compared against.
 *
 * <p>The resolution strategy (e.g., "last known good", "git base branch", "semver lower bound")
 * is determined by the resolver implementation, not this value object.
 *
 * @param versionId  Stable identifier for the resolved version (e.g. commit SHA or semver)
 * @param source     A human-readable description of how this version was resolved
 * @param label      Optional display label (e.g. "v1.2.3" or "main branch tip")
 * @param resolvedAt Timestamp when the version was resolved
 */
public record BaseVersion(
    String versionId,
    String source,
    String label,
    Instant resolvedAt
) {
    public BaseVersion {
        Objects.requireNonNull(versionId, "versionId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
        if (versionId.isBlank()) throw new IllegalArgumentException("versionId must not be blank");
        if (source.isBlank()) throw new IllegalArgumentException("source must not be blank");
    }

    /**
     * Resolution strategy enum for documenting how this version was resolved.
     */
    public enum ResolutionStrategy {
        /** Resolved against the most recently ingested version of the same spec. */
        LAST_KNOWN_GOOD,
        /** Resolved against the git base branch (e.g. main or master). */
        GIT_BASE_BRANCH,
        /** Resolved against a specific pinned version. */
        PINNED_VERSION,
        /** Resolved via a custom strategy (details in {@link #source}). */
        CUSTOM
    }
}
