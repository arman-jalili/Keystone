// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Response DTO for a single API inventory item in the Dashboard inventory view.
 *
 * <p>Returned by {@code GET /api/v1/ingestion/apis}.
 *
 * @param id            Unique spec identifier
 * @param serviceName   Service name (derived from repository)
 * @param version       Latest ingested version
 * @param specFormat    OpenAPI format (e.g. "OpenAPI 3.0")
 * @param health        Health status label
 * @param lastAnalyzed  Timestamp of last analysis
 * @param owner         Owner label (from repository path)
 * @param policyPassRate Optional policy pass rate percentage
 * @param openBreakages Optional count of open breaking changes
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiInventoryItem(
        UUID id,
        @JsonProperty("service_name") String serviceName,
        String version,
        @JsonProperty("spec_format") String specFormat,
        String health,
        @JsonProperty("last_analyzed") Instant lastAnalyzed,
        String owner,
        @JsonProperty("policy_pass_rate") Integer policyPassRate,
        @JsonProperty("open_breakages") Integer openBreakages) {

    public ApiInventoryItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(specFormat, "specFormat must not be null");
        Objects.requireNonNull(health, "health must not be null");
        Objects.requireNonNull(lastAnalyzed, "lastAnalyzed must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
    }
}
