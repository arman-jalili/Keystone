// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
package com.keystone.dashboard.infrastructure.repository.impl;

import com.keystone.dashboard.domain.model.HealthScore;
import com.keystone.dashboard.domain.model.HealthTrend;
import com.keystone.dashboard.infrastructure.repository.HealthScoreRepository;
import com.keystone.dashboard.infrastructure.repository.SpringDataHealthScoreRepository;
import com.keystone.dashboard.infrastructure.repository.jpa.HealthScoreEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of {@link HealthScoreRepository}.
 *
 * <p>Persists health scores to the database. Data survives restarts.
 */
@Repository
@Transactional
public class HealthScoreRepositoryImpl implements HealthScoreRepository {

    private final SpringDataHealthScoreRepository jpaRepository;

    public HealthScoreRepositoryImpl(SpringDataHealthScoreRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HealthScore> findLatestByEntity(String entityType, String entityId) {
        var entities = jpaRepository.findByEntityOrderByComputedAtDesc(
                entityType, entityId, PageRequest.of(0, 1));
        return entities.isEmpty() ? Optional.empty() : Optional.of(toDomain(entities.getFirst()));
    }

    @Override
    @Transactional(readOnly = true)
    public HealthTrend findTrendByEntity(String entityType, String entityId, int limit) {
        var entities = jpaRepository.findByEntityOrderByComputedAtDesc(
                entityType, entityId, PageRequest.of(0, limit));

        var points = entities.stream()
                .sorted((a, b) -> a.getComputedAt().compareTo(b.getComputedAt()))
                .map(e -> new HealthTrend.ScoreDataPoint(e.getComputedAt(), e.getScore()))
                .toList();

        HealthTrend.TrendDirection trend;
        if (points.size() < 2) {
            trend = HealthTrend.TrendDirection.INSUFFICIENT_DATA;
        } else {
            var first = points.getFirst().score();
            var last = points.getLast().score();
            double delta = last - first;
            double threshold = 0.01;
            if (Math.abs(delta) < threshold) {
                trend = HealthTrend.TrendDirection.STABLE;
            } else if (delta > 0) {
                trend = HealthTrend.TrendDirection.IMPROVING;
            } else {
                trend = HealthTrend.TrendDirection.DECLINING;
            }
        }

        return new HealthTrend(entityType, entityId, points, trend);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthScore> findAllLatest() {
        // Load all entities and deduplicate by entity (keep latest per entity)
        var all = jpaRepository.findAll();
        return all.stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getEntityType() + ":" + e.getEntityId(),
                        this::toDomain,
                        (a, b) -> a.computedAt().isAfter(b.computedAt()) ? a : b))
                .values()
                .stream()
                .toList();
    }

    @Override
    public HealthScore save(HealthScore score) {
        var entity = toEntity(score);
        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private HealthScore toDomain(HealthScoreEntity e) {
        return new HealthScore(
                e.getId(),
                e.getEntityType(),
                e.getEntityId(),
                e.getScore(),
                new HealthScore.HealthScoreDetail(
                        e.getComplianceScore() != null ? e.getComplianceScore() : 0.0,
                        e.getStabilityScore() != null ? e.getStabilityScore() : 0.0,
                        e.getFreshnessScore() != null ? e.getFreshnessScore() : 0.0,
                        e.getCoverageScore() != null ? e.getCoverageScore() : 0.0),
                e.getComputedAt());
    }

    private HealthScoreEntity toEntity(HealthScore s) {
        return new HealthScoreEntity(
                s.id(),
                s.entityType(),
                s.entityId(),
                s.score(),
                s.scoreDetail().complianceScore(),
                s.scoreDetail().stabilityScore(),
                s.scoreDetail().freshnessScore(),
                s.scoreDetail().coverageScore(),
                s.computedAt());
    }
}
