package com.keystone.dashboard.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Response DTO for a health score trend query.
 *
 * <p>Provides time-series data points and a trend direction for rendering
 * health trend charts in the dashboard.
 *
 * @param entityType  The type of entity being tracked
 * @param entityId    The identifier of the entity
 * @param dataPoints  Time-ordered data points (oldest first)
 * @param trend       Directional indicator
 */
public record HealthTrendResponse(
        @NotBlank @JsonProperty("entity_type") String entityType,
        @NotBlank @JsonProperty("entity_id") String entityId,
        List<DataPoint> dataPoints,
        String trend) {

    public HealthTrendResponse {
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        Objects.requireNonNull(dataPoints, "dataPoints must not be null");
        Objects.requireNonNull(trend, "trend must not be null");
    }

    /**
     * A single data point in a health score trend.
     *
     * @param timestamp When the score was recorded
     * @param score     The score value at that time
     */
    public record DataPoint(Instant timestamp, double score) {
        public DataPoint {
            Objects.requireNonNull(timestamp, "timestamp must not be null");
        }
    }
}
