// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Response DTO for a stale API spec item in the Dashboard stale APIs view.
 *
 * <p>Returned by {@code GET /api/v1/ingestion/apis/stale}.
 *
 * @param id           Spec identifier
 * @param serviceName  Service name (derived from repository)
 * @param lastIngested Timestamp of the most recent ingestion
 * @param daysStale    Number of days since last ingestion
 * @param version      Latest ingested version
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaleApiItem(
        UUID id,
        @JsonProperty("service_name") String serviceName,
        @JsonProperty("last_ingested") Instant lastIngested,
        @JsonProperty("days_stale") long daysStale,
        String version) {

    public StaleApiItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(lastIngested, "lastIngested must not be null");
        Objects.requireNonNull(version, "version must not be null");
    }
}
