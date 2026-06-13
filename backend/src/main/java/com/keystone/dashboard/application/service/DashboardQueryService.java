package com.keystone.dashboard.application.service;

import com.keystone.dashboard.application.dto.DashboardSummaryResponse;
import com.keystone.dashboard.application.dto.DashboardTimeRangeRequest;
import com.keystone.dashboard.application.dto.HealthScoreResponse;
import com.keystone.dashboard.application.dto.HealthTrendResponse;
import com.keystone.dashboard.domain.exception.DashboardDataNotFoundException;
import com.keystone.dashboard.domain.exception.InvalidTimeRangeException;

/**
 * Application service interface for dashboard read operations.
 *
 * <p>This is the primary inbound port (driving adapter) for the
 * dashboard module. It defines the contract that the
 * {@link com.keystone.dashboard.interfaces.http.DashboardController} depends on.
 *
 * <p>Handles all dashboard read queries including health score retrieval,
 * trend analysis, and the aggregate dashboard summary.
 */
public interface DashboardQueryService {

    /**
     * Retrieves the aggregate dashboard summary.
     *
     * <p>Provides a high-level overview including overall health score,
     * per-repository summaries, and key metrics.
     *
     * @return the dashboard summary response
     */
    DashboardSummaryResponse getDashboardSummary();

    /**
     * Retrieves the latest health score for a specific entity.
     *
     * @param entityType the type of entity (e.g. "repository", "spec", "policy-set")
     * @param entityId   the entity identifier
     * @return the latest health score
     * @throws DashboardDataNotFoundException if no health score exists for the entity
     */
    HealthScoreResponse getHealthScore(String entityType, String entityId)
            throws DashboardDataNotFoundException;

    /**
     * Retrieves the health score trend for an entity over a time range.
     *
     * @param entityType the type of entity
     * @param entityId   the entity identifier
     * @param range      the time range for trend data
     * @return the health trend with data points
     * @throws DashboardDataNotFoundException if no trend data exists
     * @throws InvalidTimeRangeException      if the time range is invalid
     */
    HealthTrendResponse getHealthTrend(
            String entityType, String entityId, DashboardTimeRangeRequest range)
            throws DashboardDataNotFoundException, InvalidTimeRangeException;
}
