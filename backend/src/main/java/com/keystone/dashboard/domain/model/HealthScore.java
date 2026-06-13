package com.keystone.dashboard.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a computed health score for a tracked entity.
 *
 * <p>A health score is a numeric value between 0.0 and 1.0 that reflects the
 * overall health of a repository, spec, or policy set. It is computed by
 * aggregating metrics such as breaking change frequency, policy compliance,
 * and spec freshness.
 *
 * @param id          Unique identifier for this health score record
 * @param entityType  The type of entity being scored (e.g. "repository", "spec", "policy-set")
 * @param entityId    The identifier of the entity within its type namespace
 * @param score       The computed health score in range [0.0, 1.0]
 * @param scoreDetail Breakdown of contributing sub-scores
 * @param computedAt  Timestamp of when the score was computed
 */
public record HealthScore(
        UUID id,
        String entityType,
        String entityId,
        double score,
        HealthScoreDetail scoreDetail,
        Instant computedAt) {

    public HealthScore {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        if (Double.isNaN(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be in range [0.0, 1.0]");
        }
        Objects.requireNonNull(scoreDetail, "scoreDetail must not be null");
        Objects.requireNonNull(computedAt, "computedAt must not be null");
    }

    /**
     * Breakdown of contributing sub-scores that compose the overall health score.
     *
     * @param complianceScore   Policy compliance contribution [0.0-1.0]
     * @param stabilityScore    API stability / breaking change frequency [0.0-1.0]
     * @param freshnessScore    Spec freshness / staleness [0.0-1.0]
     * @param coverageScore     Test or endpoint coverage contribution [0.0-1.0]
     */
    public record HealthScoreDetail(
            double complianceScore,
            double stabilityScore,
            double freshnessScore,
            double coverageScore) {

        public HealthScoreDetail {
            validateSubScore(complianceScore, "complianceScore");
            validateSubScore(stabilityScore, "stabilityScore");
            validateSubScore(freshnessScore, "freshnessScore");
            validateSubScore(coverageScore, "coverageScore");
        }

        private static void validateSubScore(double value, String name) {
            if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException(name + " must be in range [0.0, 1.0]");
            }
        }
    }
}
