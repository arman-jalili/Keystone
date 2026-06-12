package com.keystone.breaking.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a single breaking change finding.
 *
 * <p>Each instance captures one atomic breaking change detected during
 * diff analysis. Multiple {@code BreakingChange} instances are aggregated
 * into a {@link DiffReport}.
 *
 * @param id          Unique identifier for this finding
 * @param type        The category of breaking change
 * @param severity    The severity level (breaking, warn, info)
 * @param path        JSON Pointer or dot-notation path to the changed element
 * @param oldValue    The value in the base (older) specification
 * @param newValue    The value in the new (current) specification
 * @param description Human-readable description of the change
 * @param detectedAt  Timestamp when this change was detected
 */
public record BreakingChange(
    UUID id,
    ChangeType type,
    Severity severity,
    String path,
    String oldValue,
    String newValue,
    String description,
    Instant detectedAt
) {
    public BreakingChange {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(detectedAt, "detectedAt must not be null");
        if (path.isBlank()) throw new IllegalArgumentException("path must not be blank");
        if (description.isBlank()) throw new IllegalArgumentException("description must not be blank");
    }

    /**
     * Severity levels for breaking changes.
     */
    public enum Severity {
        /** Definitely breaks existing clients — must be addressed before release. */
        BREAKING,
        /** Likely breaks clients depending on interpretation — requires manual review. */
        WARNING,
        /** Informational: changed but unlikely to break clients. */
        INFO
    }
}
