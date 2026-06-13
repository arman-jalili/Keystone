package com.keystone.dashboard.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Objects;

/**
 * Response DTO for the dashboard summary endpoint.
 *
 * <p>Provides a high-level overview of all tracked entities for the
 * main dashboard view.
 *
 * @param overallScore    Aggregate health score across all tracked entities
 * @param repositories    Per-repository health summaries
 * @param totalSpecs      Total number of tracked specifications
 * @param activePolicies  Total number of active policies
 * @param recentViolations Count of violations detected in the recent window
 */
public record DashboardSummaryResponse(
        @JsonProperty("overall_score") double overallScore,
        List<RepositoryHealthItem> repositories,
        @PositiveOrZero int totalSpecs,
        @PositiveOrZero int activePolicies,
        @PositiveOrZero int recentViolations) {

    public DashboardSummaryResponse {
        Objects.requireNonNull(repositories, "repositories must not be null");
    }

    /**
     * A single repository health item in the dashboard summary.
     *
     * @param repositoryId   Repository identifier (e.g. "org/repo")
     * @param healthScore    Current composite health score [0.0-1.0]
     * @param specCount      Number of specs tracked for this repository
     * @param violationCount Number of active violations
     * @param trend          Trend direction indicator
     */
    public record RepositoryHealthItem(
            @NotBlank String repositoryId,
            @JsonProperty("health_score") double healthScore,
            @PositiveOrZero int specCount,
            @PositiveOrZero int violationCount,
            String trend) {
        public RepositoryHealthItem {
            Objects.requireNonNull(repositoryId, "repositoryId must not be null");
            Objects.requireNonNull(trend, "trend must not be null");
        }
    }
}
