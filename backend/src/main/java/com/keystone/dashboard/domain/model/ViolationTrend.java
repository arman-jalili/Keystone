// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Domain model for violation trend data points
package com.keystone.dashboard.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A single data point in a violation trend over time.
 *
 * @param date          The date of this data point
 * @param violationCount Number of violations recorded on this date
 * @param severity      The severity level for this data point
 */
public record ViolationTrend(Instant date, int violationCount, String severity) {

    public ViolationTrend {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
    }
}
