// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Domain service for computing health scores from raw sub-scores
package com.keystone.dashboard.domain.service.impl;

import com.keystone.dashboard.domain.exception.HealthScoreComputationException;
import com.keystone.dashboard.domain.model.HealthScore;
import com.keystone.dashboard.domain.service.HealthScoreCalculator;
import org.springframework.stereotype.Service;

/**
 * Computes health scores using weighted averaging of sub-scores.
 *
 * <p>The algorithm:
 * <ol>
 *   <li>Validates all sub-scores are in range [0.0, 1.0]</li>
 *   <li>Applies weights: compliance 35%, stability 25%, freshness 20%, coverage 20%</li>
 *   <li>Clamps the result to [0.0, 1.0]</li>
 * </ol>
 */
@Service
public class HealthScoreCalculatorImpl implements HealthScoreCalculator {

    private static final double COMPLIANCE_WEIGHT = 0.35;
    private static final double STABILITY_WEIGHT = 0.25;
    private static final double FRESHNESS_WEIGHT = 0.20;
    private static final double COVERAGE_WEIGHT = 0.20;

    @Override
    public HealthScore.HealthScoreDetail computeSubScores(
            double complianceScore, double stabilityScore, double freshnessScore, double coverageScore)
            throws HealthScoreComputationException {
        try {
            return new HealthScore.HealthScoreDetail(complianceScore, stabilityScore, freshnessScore, coverageScore);
        } catch (IllegalArgumentException e) {
            throw new HealthScoreComputationException(
                    "Invalid sub-score values: " + e.getMessage(), "generic", "generic");
        }
    }

    @Override
    public double aggregateScore(HealthScore.HealthScoreDetail detail) {
        return clamp(detail.complianceScore() * COMPLIANCE_WEIGHT
                + detail.stabilityScore() * STABILITY_WEIGHT
                + detail.freshnessScore() * FRESHNESS_WEIGHT
                + detail.coverageScore() * COVERAGE_WEIGHT);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
