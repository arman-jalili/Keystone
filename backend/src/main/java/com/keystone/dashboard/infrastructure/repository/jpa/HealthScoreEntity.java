// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
package com.keystone.dashboard.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code health_scores} table.
 */
@Entity
@Table(name = "health_scores")
public class HealthScoreEntity {

    @Id
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 256)
    private String entityId;

    @Column(nullable = false)
    private double score;

    @Column(name = "compliance_score")
    private Double complianceScore;

    @Column(name = "stability_score")
    private Double stabilityScore;

    @Column(name = "freshness_score")
    private Double freshnessScore;

    @Column(name = "coverage_score")
    private Double coverageScore;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    protected HealthScoreEntity() {}

    public HealthScoreEntity(
            UUID id,
            String entityType,
            String entityId,
            double score,
            Double complianceScore,
            Double stabilityScore,
            Double freshnessScore,
            Double coverageScore,
            Instant computedAt) {
        this.id = Objects.requireNonNull(id);
        this.entityType = Objects.requireNonNull(entityType);
        this.entityId = Objects.requireNonNull(entityId);
        this.score = score;
        this.complianceScore = complianceScore;
        this.stabilityScore = stabilityScore;
        this.freshnessScore = freshnessScore;
        this.coverageScore = coverageScore;
        this.computedAt = Objects.requireNonNull(computedAt);
    }

    public UUID getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public double getScore() {
        return score;
    }

    public Double getComplianceScore() {
        return complianceScore;
    }

    public Double getStabilityScore() {
        return stabilityScore;
    }

    public Double getFreshnessScore() {
        return freshnessScore;
    }

    public Double getCoverageScore() {
        return coverageScore;
    }

    public Instant getComputedAt() {
        return computedAt;
    }
}
