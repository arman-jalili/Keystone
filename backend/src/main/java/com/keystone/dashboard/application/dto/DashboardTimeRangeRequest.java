// Canonical Reference: .pi/architecture/modules/dashboard.md#dashboard-controller
// Implements: Request DTO for time range filtering
package com.keystone.dashboard.application.dto;

import jakarta.validation.constraints.Pattern;
import java.util.Objects;

/**
 * Request DTO for filtering dashboard data by time range.
 *
 * <p>Supports predefined range types or explicit start/end timestamps.
 * Either {@code range} or both {@code start} and {@code end} must be provided.
 *
 * @param range Predefined range type (e.g. "LAST_7_DAYS", "LAST_30_DAYS")
 * @param start ISO-8601 start timestamp (required if range is "CUSTOM")
 * @param end   ISO-8601 end timestamp (required if range is "CUSTOM")
 */
public record DashboardTimeRangeRequest(
        @Pattern(
                        regexp = "LAST_24_HOURS|LAST_7_DAYS|LAST_30_DAYS|LAST_90_DAYS|CUSTOM",
                        message =
                                "range must be one of: LAST_24_HOURS, LAST_7_DAYS, LAST_30_DAYS, LAST_90_DAYS, CUSTOM")
                String range,
        String start,
        String end) {

    public DashboardTimeRangeRequest {
        if ("CUSTOM".equals(range)) {
            Objects.requireNonNull(start, "start is required when range is CUSTOM");
            Objects.requireNonNull(end, "end is required when range is CUSTOM");
        }
    }
}
