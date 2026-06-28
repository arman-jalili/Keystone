// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Application service interface for health score computation and retrieval
package com.keystone.dashboard.application.service;

import com.keystone.dashboard.application.dto.DashboardTimeRangeRequest;
import com.keystone.dashboard.application.dto.HealthScoreResponse;
import com.keystone.dashboard.application.dto.HealthTrendResponse;
import com.keystone.dashboard.domain.exception.DashboardDataNotFoundException;
import com.keystone.dashboard.domain.exception.HealthScoreComputationException;
import com.keystone.dashboard.domain.exception.InvalidTimeRangeException;
import com.keystone.dashboard.domain.model.ComplianceSummary;
import com.keystone.dashboard.domain.model.GovernanceHealthScore;
import com.keystone.dashboard.domain.model.HealthScore;
import com.keystone.dashboard.domain.model.ViolationTrend;
import java.util.List;

/**
 * Application service interface for the health score computation use case.
 *
 * <p>This is the primary inbound port (driving adapter) for the
 * health score component. It defines the contract that callers —
 * including {@link DashboardQueryService} and scheduled jobs — depend on.
 *
 * <p>Orchestrates the health score flow:
 * <ol>
 *   <li>Gather raw metrics from data sources</li>
 *   <li>Delegate to {@link com.keystone.dashboard.domain.service.HealthScoreCalculator} for computation</li>
 *   <li>Persist via {@link com.keystone.dashboard.infrastructure.repository.HealthScoreRepository}</li>
 *   <li>Publish {@link com.keystone.dashboard.domain.event.HealthScoreRecalculatedEvent}</li>
 * </ol>
 */
public interface HealthScoreService {

    /**
     * Computes and persists a health score for the given entity.
     *
     * <p>This is typically called by a scheduled job or triggered by
     * domain events from other modules (e.g. spec ingestion, policy evaluation).
     *
     * @param entityType      the type of entity to score
     * @param entityId        the entity identifier
     * @param complianceScore compliance sub-score [0.0-1.0]
     * @param stabilityScore  stability sub-score [0.0-1.0]
     * @param freshnessScore  freshness sub-score [0.0-1.0]
     * @param coverageScore   coverage sub-score [0.0-1.0]
     * @return the computed and persisted health score
     * @throws HealthScoreComputationException if computation fails
     */
    HealthScore computeAndPersistScore(
            String entityType,
            String entityId,
            double complianceScore,
            double stabilityScore,
            double freshnessScore,
            double coverageScore)
            throws HealthScoreComputationException;

    /**
     * Retrieves the latest health score for an entity.
     *
     * @param entityType the type of entity
     * @param entityId   the entity identifier
     * @return the latest health score response
     * @throws DashboardDataNotFoundException if no score has been computed yet
     */
    HealthScoreResponse getHealthScore(String entityType, String entityId) throws DashboardDataNotFoundException;

    /**
     * Retrieves the health score trend for an entity over a time range.
     *
     * @param entityType the type of entity
     * @param entityId   the entity identifier
     * @param range      the time range for trend data
     * @return the health trend response
     * @throws DashboardDataNotFoundException if no trend data exists
     * @throws InvalidTimeRangeException      if the time range is invalid
     */
    HealthTrendResponse getHealthTrend(String entityType, String entityId, DashboardTimeRangeRequest range)
            throws DashboardDataNotFoundException, InvalidTimeRangeException;

    /**
     * Computes the GovernanceHealthScore from raw metrics across all contexts.
     *
     * @param period the time period (e.g. "LAST_30_DAYS", "LAST_90_DAYS")
     * @return the computed governance health score
     */
    GovernanceHealthScore calculate(String period);

    /**
     * Retrieves compliance history for a specific spec.
     *
     * @param specId the spec identifier
     * @param days   number of days of history to retrieve
     * @return list of compliance summaries
     */
    List<ComplianceSummary> getComplianceHistory(String specId, int days);

    /**
     * Retrieves violation trends over the specified number of days.
     *
     * @param days number of days of trend data
     * @return list of violation trend data points
     */
    List<ViolationTrend> getViolationTrends(int days);
}
