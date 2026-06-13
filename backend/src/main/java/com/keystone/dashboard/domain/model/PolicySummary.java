package com.keystone.dashboard.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a policy summary for the policy UI.
 *
 * <p>Provides a concise view of a single policy for display in the
 * Policy UI component — including its current status, severity,
 * and recent evaluation result.
 *
 * @param id            Unique policy identifier
 * @param name          Human-readable policy name
 * @param description   Optional policy description
 * @param status        Current policy lifecycle status
 * @param severity      Assigned severity level
 * @param violationCount Number of active violations
 * @param lastEvaluated Timestamp of the most recent evaluation
 */
public record PolicySummary(
        UUID id,
        String name,
        String description,
        PolicyStatus status,
        PolicySeverity severity,
        int violationCount,
        Instant lastEvaluated) {

    public PolicySummary {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        if (violationCount < 0) {
            throw new IllegalArgumentException("violationCount must not be negative");
        }
    }

    /**
     * Lifecycle status of a policy.
     */
    public enum PolicyStatus {
        ACTIVE,
        INACTIVE,
        DRAFT,
        ARCHIVED,
        ERROR
    }

    /**
     * Severity level assigned to a policy.
     */
    public enum PolicySeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }
}
