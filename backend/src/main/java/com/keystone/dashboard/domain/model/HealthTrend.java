// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Domain model for health score time-series trends
package com.keystone.dashboard.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain model representing a time-series trend of health scores.
 *
 * <p>Provides the data points needed to render a health trend chart
 * in the dashboard. Each data point is a snapshot of the health score
 * at a given point in time.
 *
 * @param entityType  The type of entity being tracked
 * @param entityId    The identifier of the entity
 * @param dataPoints  Time-ordered list of score snapshots (oldest first)
 * @param trend       Directional indicator of the overall trend
 */
public record HealthTrend(String entityType, String entityId, List<ScoreDataPoint> dataPoints, TrendDirection trend) {

    public HealthTrend {
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        Objects.requireNonNull(dataPoints, "dataPoints must not be null");
        Objects.requireNonNull(trend, "trend must not be null");
    }

    /**
     * Returns an unmodifiable view of the data points.
     */
    public List<ScoreDataPoint> dataPoints() {
        return Collections.unmodifiableList(dataPoints);
    }

    /**
     * A single data point in a health score trend.
     *
     * @param timestamp When the score was recorded
     * @param score     The score value at that time
     */
    public record ScoreDataPoint(Instant timestamp, double score) {
        public ScoreDataPoint {
            Objects.requireNonNull(timestamp, "timestamp must not be null");
            if (Double.isNaN(score) || score < 0.0 || score > 1.0) {
                throw new IllegalArgumentException("score must be in range [0.0, 1.0]");
            }
        }
    }

    /**
     * Directional indicator for a health score trend.
     */
    public enum TrendDirection {
        /** Score is improving over the observed window. */
        IMPROVING,
        /** Score is declining over the observed window. */
        DECLINING,
        /** Score is stable / no significant change. */
        STABLE,
        /** Insufficient data to determine a trend. */
        INSUFFICIENT_DATA
    }
}
