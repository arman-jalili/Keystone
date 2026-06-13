package com.keystone.dashboard.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Objects;

/**
 * Response DTO for policy breakdown data in the Policy UI.
 *
 * @param totalPolicies     Total number of policies tracked
 * @param byStatus          Policies grouped by lifecycle status
 * @param bySeverity        Policies grouped by severity level
 * @param overallCompliance Overall compliance rate [0.0-1.0]
 */
public record PolicyBreakdownResponse(
        @PositiveOrZero @JsonProperty("total_policies") int totalPolicies,
        List<StatusGroup> byStatus,
        List<SeverityGroup> bySeverity,
        @JsonProperty("overall_compliance") double overallCompliance) {

    public PolicyBreakdownResponse {
        Objects.requireNonNull(byStatus, "byStatus must not be null");
        Objects.requireNonNull(bySeverity, "bySeverity must not be null");
    }

    /**
     * Policies grouped by status.
     *
     * @param status The policy status
     * @param count  Number of policies in this status
     */
    public record StatusGroup(String status, @PositiveOrZero int count) {
        public StatusGroup {
            Objects.requireNonNull(status, "status must not be null");
        }
    }

    /**
     * Policies grouped by severity.
     *
     * @param severity The severity level
     * @param count    Number of policies at this severity
     */
    public record SeverityGroup(String severity, @PositiveOrZero int count) {
        public SeverityGroup {
            Objects.requireNonNull(severity, "severity must not be null");
        }
    }
}
