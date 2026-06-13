// Canonical Reference: .pi/architecture/modules/dashboard.md#dashboard-controller
// Implements: Domain model for aggregate dashboard overview
package com.keystone.dashboard.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain model representing the aggregate dashboard overview.
 *
 * <p>Provides a high-level summary of all tracked repositories and policy sets,
 * including overall health and key metrics. This is the primary data model
 * for the main dashboard view.
 *
 * @param overallScore  Aggregate health score across all tracked entities
 * @param repositories  Per-repository health summaries
 * @param totalSpecs    Total number of tracked specifications
 * @param activePolicies Total number of active policies
 * @param recentViolations Count of violations detected in the recent window
 */
public record DashboardSummary(
        double overallScore,
        List<RepositoryHealth> repositories,
        int totalSpecs,
        int activePolicies,
        int recentViolations) {

    public DashboardSummary {
        Objects.requireNonNull(repositories, "repositories must not be null");
    }

    /**
     * Returns an unmodifiable view of the repository health list.
     */
    public List<RepositoryHealth> repositories() {
        return Collections.unmodifiableList(repositories);
    }

    /**
     * Per-repository health summary for the dashboard.
     *
     * @param repositoryId   Repository identifier (e.g. "org/repo")
     * @param healthScore    Current health score [0.0-1.0]
     * @param specCount      Number of specs tracked for this repository
     * @param violationCount Number of active violations
     * @param trend          Health trend direction
     */
    public record RepositoryHealth(
            String repositoryId,
            double healthScore,
            int specCount,
            int violationCount,
            HealthTrend.TrendDirection trend) {

        public RepositoryHealth {
            Objects.requireNonNull(repositoryId, "repositoryId must not be null");
            if (Double.isNaN(healthScore) || healthScore < 0.0 || healthScore > 1.0) {
                throw new IllegalArgumentException("healthScore must be in range [0.0, 1.0]");
            }
            Objects.requireNonNull(trend, "trend must not be null");
        }
    }
}
