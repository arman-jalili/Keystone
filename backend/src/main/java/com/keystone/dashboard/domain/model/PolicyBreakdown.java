package com.keystone.dashboard.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain model representing a breakdown of policies by status and severity.
 *
 * <p>Used by the Policy UI to render aggregate views such as policy counts
 * grouped by status, severity distribution, and compliance overview.
 *
 * @param totalPolicies     Total number of policies tracked
 * @param byStatus          Policies grouped by lifecycle status
 * @param bySeverity        Policies grouped by severity level
 * @param overallCompliance Overall compliance rate [0.0-1.0]
 */
public record PolicyBreakdown(
        int totalPolicies, List<StatusGroup> byStatus, List<SeverityGroup> bySeverity, double overallCompliance) {

    public PolicyBreakdown {
        Objects.requireNonNull(byStatus, "byStatus must not be null");
        Objects.requireNonNull(bySeverity, "bySeverity must not be null");
        if (Double.isNaN(overallCompliance) || overallCompliance < 0.0 || overallCompliance > 1.0) {
            throw new IllegalArgumentException("overallCompliance must be in range [0.0, 1.0]");
        }
    }

    /**
     * Returns an unmodifiable view of the status groups.
     */
    public List<StatusGroup> byStatus() {
        return Collections.unmodifiableList(byStatus);
    }

    /**
     * Returns an unmodifiable view of the severity groups.
     */
    public List<SeverityGroup> bySeverity() {
        return Collections.unmodifiableList(bySeverity);
    }

    /**
     * Policies grouped by lifecycle status.
     *
     * @param status  The policy status
     * @param count   Number of policies in this status
     */
    public record StatusGroup(PolicySummary.PolicyStatus status, int count) {
        public StatusGroup {
            Objects.requireNonNull(status, "status must not be null");
            if (count < 0) {
                throw new IllegalArgumentException("count must not be negative");
            }
        }
    }

    /**
     * Policies grouped by severity level.
     *
     * @param severity The severity level
     * @param count    Number of policies at this severity
     */
    public record SeverityGroup(PolicySummary.PolicySeverity severity, int count) {
        public SeverityGroup {
            Objects.requireNonNull(severity, "severity must not be null");
            if (count < 0) {
                throw new IllegalArgumentException("count must not be negative");
            }
        }
    }
}
