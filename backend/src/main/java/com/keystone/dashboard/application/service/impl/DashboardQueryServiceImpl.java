// Canonical Reference: .pi/architecture/modules/dashboard.md#dashboard-controller
// Implements: Application service for dashboard read operations
package com.keystone.dashboard.application.service.impl;

import com.keystone.dashboard.application.dto.DashboardSummaryResponse;
import com.keystone.dashboard.application.dto.DashboardTimeRangeRequest;
import com.keystone.dashboard.application.dto.HealthScoreResponse;
import com.keystone.dashboard.application.dto.HealthTrendResponse;
import com.keystone.dashboard.application.service.DashboardQueryService;
import com.keystone.dashboard.domain.exception.DashboardDataNotFoundException;
import com.keystone.dashboard.domain.exception.InvalidTimeRangeException;
import com.keystone.dashboard.infrastructure.repository.DashboardMetricsRepository;
import com.keystone.dashboard.infrastructure.repository.HealthScoreRepository;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link DashboardQueryService}.
 *
 * <p>Provides read-optimized access to dashboard aggregate data
 * via the metrics and health score repositories.
 */
@Service
public class DashboardQueryServiceImpl implements DashboardQueryService {

    private final DashboardMetricsRepository metricsRepository;
    private final HealthScoreRepository healthScoreRepository;

    public DashboardQueryServiceImpl(
            DashboardMetricsRepository metricsRepository, HealthScoreRepository healthScoreRepository) {
        this.metricsRepository = metricsRepository;
        this.healthScoreRepository = healthScoreRepository;
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        var summary = metricsRepository.findDashboardSummary();
        var repositories = summary.repositories().stream()
                .map(r -> new DashboardSummaryResponse.RepositoryHealthItem(
                        r.repositoryId(),
                        r.healthScore(),
                        r.specCount(),
                        r.violationCount(),
                        r.trend().name()))
                .collect(Collectors.toList());
        return new DashboardSummaryResponse(
                summary.overallScore(),
                repositories,
                summary.totalSpecs(),
                summary.activePolicies(),
                summary.recentViolations());
    }

    @Override
    public HealthScoreResponse getHealthScore(String entityType, String entityId)
            throws DashboardDataNotFoundException {
        return healthScoreRepository
                .findLatestByEntity(entityType, entityId)
                .map(s -> new HealthScoreResponse(
                        s.id(),
                        s.entityType(),
                        s.entityId(),
                        s.score(),
                        s.scoreDetail().complianceScore(),
                        s.scoreDetail().stabilityScore(),
                        s.scoreDetail().freshnessScore(),
                        s.scoreDetail().coverageScore(),
                        s.computedAt()))
                .orElseThrow(() -> new DashboardDataNotFoundException(
                        "Health score not found for " + entityType + "/" + entityId, entityType, entityId));
    }

    @Override
    public HealthTrendResponse getHealthTrend(String entityType, String entityId, DashboardTimeRangeRequest range)
            throws DashboardDataNotFoundException, InvalidTimeRangeException {
        var trend = healthScoreRepository.findTrendByEntity(entityType, entityId, 100);
        var points = trend.dataPoints().stream()
                .map(dp -> new HealthTrendResponse.DataPoint(dp.timestamp(), dp.score()))
                .toList();
        return new HealthTrendResponse(
                entityType, entityId, points, trend.trend().name());
    }
}
