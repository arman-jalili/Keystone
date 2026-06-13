package com.keystone.dashboard.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Response DTO for a single policy summary in the Policy UI.
 *
 * @param id              Unique policy identifier
 * @param name            Human-readable policy name
 * @param description     Optional policy description
 * @param status          Current lifecycle status
 * @param severity        Assigned severity level
 * @param violationCount  Number of active violations
 * @param lastEvaluated   Timestamp of the most recent evaluation
 */
public record PolicySummaryResponse(
        UUID id,
        @NotBlank String name,
        String description,
        @NotBlank String status,
        @NotBlank String severity,
        @PositiveOrZero @JsonProperty("violation_count") int violationCount,
        @JsonProperty("last_evaluated") Instant lastEvaluated) {

    public PolicySummaryResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
    }
}
