// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Response DTO for health score queries
package com.keystone.dashboard.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Response DTO for a single health score query.
 *
 * @param id             Unique health score record identifier
 * @param entityType     The type of entity being scored
 * @param entityId       The identifier of the entity
 * @param score          The composite health score [0.0-1.0]
 * @param compliance     Compliance sub-score
 * @param stability      Stability sub-score
 * @param freshness      Freshness sub-score
 * @param coverage       Coverage sub-score
 * @param computedAt     Timestamp of the computation
 */
public record HealthScoreResponse(
        UUID id,
        @NotBlank @JsonProperty("entity_type") String entityType,
        @NotBlank @JsonProperty("entity_id") String entityId,
        double score,
        double compliance,
        double stability,
        double freshness,
        double coverage,
        @JsonProperty("computed_at") Instant computedAt) {

    public HealthScoreResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        Objects.requireNonNull(computedAt, "computedAt must not be null");
    }
}
