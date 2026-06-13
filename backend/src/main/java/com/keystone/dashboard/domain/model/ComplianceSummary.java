// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Domain model for spec compliance history
package com.keystone.dashboard.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Compliance summary for a single spec over a time range.
 *
 * @param specId          The spec identifier
 * @param specPath        The relative path of the spec
 * @param repository      The repository identifier
 * @param lastEvaluated   Timestamp of the most recent evaluation
 * @param complianceRate  Compliance rate [0.0-1.0]
 * @param violationCount  Number of active violations
 */
public record ComplianceSummary(
        String specId,
        String specPath,
        String repository,
        Instant lastEvaluated,
        double complianceRate,
        int violationCount) {

    public ComplianceSummary {
        Objects.requireNonNull(specId, "specId must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(lastEvaluated, "lastEvaluated must not be null");
    }
}
