// Canonical Reference: .pi/architecture/modules/dashboard.md#dashboard-controller
// Implements: Repository implementation for aggregate dashboard metrics
package com.keystone.dashboard.infrastructure.repository.impl;

import com.keystone.dashboard.domain.model.DashboardMetric;
import com.keystone.dashboard.domain.model.DashboardSummary;
import com.keystone.dashboard.domain.model.HealthTrend;
import com.keystone.dashboard.domain.model.PolicyBreakdown;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link com.keystone.dashboard.infrastructure.repository.DashboardMetricsRepository}.
 *
 * <p>Returns empty/default data. In production, this should query
 * materialized views or aggregate tables.
 */
@Repository
public class DashboardMetricsRepositoryImpl
        implements com.keystone.dashboard.infrastructure.repository.DashboardMetricsRepository {

    @Override
    public DashboardSummary findDashboardSummary() {
        return new DashboardSummary(0.0, Collections.emptyList(), 0, 0, 0);
    }

    @Override
    public List<DashboardMetric> findMetrics() {
        return Collections.emptyList();
    }

    @Override
    public PolicyBreakdown findPolicyBreakdown() {
        return new PolicyBreakdown(0, Collections.emptyList(), Collections.emptyList(), 1.0);
    }
}
