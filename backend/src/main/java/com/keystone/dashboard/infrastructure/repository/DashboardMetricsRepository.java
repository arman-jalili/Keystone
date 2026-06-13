package com.keystone.dashboard.infrastructure.repository;

import com.keystone.dashboard.domain.model.DashboardMetric;
import com.keystone.dashboard.domain.model.DashboardSummary;
import com.keystone.dashboard.domain.model.PolicyBreakdown;
import java.util.List;

/**
 * Repository interface for accessing aggregate dashboard metrics.
 *
 * <p>This repository provides pre-computed aggregate data for the dashboard
 * views. The underlying implementation may use materialized views, cached
 * query results, or read replicas.
 *
 * <p>This is a read-optimized repository — all methods are read-only.
 */
public interface DashboardMetricsRepository {

    /**
     * Retrieves the overall dashboard summary.
     *
     * @return the aggregate dashboard overview
     */
    DashboardSummary findDashboardSummary();

    /**
     * Retrieves the current KPI metrics for the dashboard.
     *
     * @return list of key performance metrics
     */
    List<DashboardMetric> findMetrics();

    /**
     * Retrieves the policy breakdown by status and severity.
     *
     * @return policy breakdown data
     */
    PolicyBreakdown findPolicyBreakdown();
}
