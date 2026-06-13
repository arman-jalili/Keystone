// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a single detected change between two API specifications.
 *
 * <p>Produced by {@link com.keystone.analysis.domain.detector.ChangeDetector} implementations
 * and aggregated into a {@link BreakingChangeReport}.
 *
 * @param id          Unique identifier for this change
 * @param severity    The change severity classification
 * @param path        JSON Pointer or dot-notation path to the changed element
 * @param oldValue    The value in the base (older) specification
 * @param newValue    The value in the new (target) specification
 * @param message     Human-readable description of the change
 * @param detectorName Name of the detector that found this change
 */
public record Change(
        UUID id,
        ChangeSeverity severity,
        String path,
        String oldValue,
        String newValue,
        String message,
        String detectorName) {
    public Change {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(detectorName, "detectorName must not be null");
        if (path.isBlank()) throw new IllegalArgumentException("path must not be blank");
        if (message.isBlank()) throw new IllegalArgumentException("message must not be blank");
    }
}
