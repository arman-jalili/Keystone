package com.keystone.dashboard.domain.service;

import com.keystone.dashboard.domain.exception.HealthScoreComputationException;
import com.keystone.dashboard.domain.model.HealthScore;

/**
 * Domain service interface for computing health scores.
 *
 * <p>This is the core domain logic for the HealthScoreService component.
 * It encapsulates the algorithm that aggregates sub-scores (compliance,
 * stability, freshness, coverage) into a single health score value.
 *
 * <p>The calculator is stateless — it computes a score from raw input
 * data without side effects. Callers provide the raw metrics and the
 * calculator returns the computed score.
 */
public interface HealthScoreCalculator {

    /**
     * Computes a health score from raw sub-score inputs.
     *
     * <p>The algorithm is:
     * <ol>
     *   <li>Validate all sub-scores are in range [0.0, 1.0]</li>
     *   <li>Apply a weighted average with configurable weights</li>
     *   <li>Clamp the result to [0.0, 1.0]</li>
     * </ol>
     *
     * @param complianceScore Policy compliance contribution [0.0-1.0]
     * @param stabilityScore  API stability contribution [0.0-1.0]
     * @param freshnessScore  Spec freshness contribution [0.0-1.0]
     * @param coverageScore   Coverage contribution [0.0-1.0]
     * @return the computed health score
     * @throws HealthScoreComputationException if computation fails
     */
    HealthScore.HealthScoreDetail computeSubScores(
            double complianceScore,
            double stabilityScore,
            double freshnessScore,
            double coverageScore)
            throws HealthScoreComputationException;

    /**
     * Computes an aggregate score from a detail breakdown.
     *
     * @param detail The sub-score breakdown
     * @return aggregated score in range [0.0, 1.0]
     */
    double aggregateScore(HealthScore.HealthScoreDetail detail);
}
