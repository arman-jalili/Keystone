// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Domain model for the computed governance health score
package com.keystone.dashboard.domain.model;

import java.util.Objects;

/**
 * Domain model representing the computed GovernanceHealthScore.
 *
 * <p>Aggregates spec compliance rate, policy pass rate, and exemption rate
 * into a single health score using weighted averaging.
 *
 * @param score              The composite health score [0.0-1.0]
 * @param period             The time period over which the score was computed (e.g. "LAST_30_DAYS")
 * @param totalSpecs         Total number of specs ingested in the period
 * @param specComplianceRate Spec compliance rate [0.0-1.0]
 * @param policyPassRate     Policy evaluation pass rate [0.0-1.0]
 * @param exemptionRate      Active exemption rate [0.0-1.0]
 */
public record GovernanceHealthScore(
        double score,
        String period,
        long totalSpecs,
        double specComplianceRate,
        double policyPassRate,
        double exemptionRate) {

    public GovernanceHealthScore {
        Objects.requireNonNull(period, "period must not be null");
    }
}
