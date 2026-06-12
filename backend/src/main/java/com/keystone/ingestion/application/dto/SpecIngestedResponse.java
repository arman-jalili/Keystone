package com.keystone.ingestion.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Response DTO returned when a spec has been successfully ingested.
 *
 * <p>If the spec is a duplicate, the same {@code eventId} is returned
 * (idempotent behavior per ADR-007). The HTTP status differs:
 * <ul>
 *   <li><strong>201 Created</strong> — new ingestion</li>
 *   <li><strong>200 OK</strong> — duplicate, eventId references the original</li>
 * </ul>
 *
 * @param eventId      The unique event identifier for this ingestion
 * @param specId       The UUID of the ingested OpenApiSpec
 * @param repository   The repository identifier
 * @param specPath     The relative spec path within the repository
 * @param commitSha    The git commit SHA
 * @param checksum     Checksum of the ingested content
 * @param ingestedAt   ISO-8601 timestamp of ingestion
 */
public record SpecIngestedResponse(
    UUID eventId,
    UUID specId,
    String repository,
    String specPath,
    String commitSha,
    String checksum,
    Instant ingestedAt
) {
    public SpecIngestedResponse {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(specId, "specId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        Objects.requireNonNull(checksum, "checksum must not be null");
        Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");
    }
}
