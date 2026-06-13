// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing a resolved base version for comparison.
 *
 * <p>Produced by {@link com.keystone.analysis.domain.service.BaseVersionResolver}
 * and consumed by {@link com.keystone.analysis.domain.service.DiffOrchestrator}
 * to determine which specification version the incoming spec should be compared against.
 *
 * @param versionId  Stable identifier for the resolved version (e.g. commit SHA or semver)
 * @param source     A human-readable description of how this version was resolved
 * @param label      Optional display label (e.g. "v1.2.3" or "main branch tip")
 * @param resolvedAt Timestamp when the version was resolved
 */
public record BaseVersion(String versionId, String source, String label, Instant resolvedAt) {
    public BaseVersion {
        Objects.requireNonNull(versionId, "versionId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
        if (versionId.isBlank()) throw new IllegalArgumentException("versionId must not be blank");
        if (source.isBlank()) throw new IllegalArgumentException("source must not be blank");
    }
}
