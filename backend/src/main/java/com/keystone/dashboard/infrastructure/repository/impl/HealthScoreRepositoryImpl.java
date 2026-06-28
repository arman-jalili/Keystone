// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Repository implementation for health score data access
package com.keystone.dashboard.infrastructure.repository.impl;

import com.keystone.dashboard.domain.model.HealthScore;
import com.keystone.dashboard.domain.model.HealthTrend;
import com.keystone.dashboard.infrastructure.repository.HealthScoreRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link HealthScoreRepository}.
 *
 * <p>Stores health scores in a thread-safe list. Suitable for development
 * and testing. In production, this should be backed by a database.
 */
@Repository
public class HealthScoreRepositoryImpl implements HealthScoreRepository {

    private final List<HealthScore> store = new CopyOnWriteArrayList<>();

    @Override
    public Optional<HealthScore> findLatestByEntity(String entityType, String entityId) {
        return store.stream()
                .filter(s -> s.entityType().equals(entityType) && s.entityId().equals(entityId))
                .max((a, b) -> a.computedAt().compareTo(b.computedAt()));
    }

    @Override
    public HealthTrend findTrendByEntity(String entityType, String entityId, int limit) {
        List<HealthScore> scores = store.stream()
                .filter(s -> s.entityType().equals(entityType) && s.entityId().equals(entityId))
                .sorted((a, b) -> a.computedAt().compareTo(b.computedAt()))
                .toList();

        List<HealthTrend.ScoreDataPoint> points = scores.stream()
                .map(s -> new HealthTrend.ScoreDataPoint(s.computedAt(), s.score()))
                .toList();

        HealthTrend.TrendDirection trend;
        if (points.size() < 2) {
            trend = HealthTrend.TrendDirection.INSUFFICIENT_DATA;
        } else {
            var first = points.getFirst().score();
            var last = points.getLast().score();
            double delta = last - first;
            double threshold = 0.01; // 1% change threshold
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
    public List<HealthScore> findAllLatest() {
        return Collections.unmodifiableList(new ArrayList<>(store));
    }

    @Override
    public HealthScore save(HealthScore score) {
        store.add(score);
        return score;
    }
}
