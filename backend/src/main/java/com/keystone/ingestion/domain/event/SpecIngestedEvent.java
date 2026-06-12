package com.keystone.ingestion.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an OpenAPI spec has been successfully ingested.
 *
 * <p>Consumers (e.g. Breaking Change Analysis) subscribe to this event to
 * trigger downstream processing. Per ADR-004, this event is also stored
 * as an immutable audit event.
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param specId         The UUID of the ingested OpenApiSpec
 * @param commitSha      The git commit SHA the spec was ingested from
 * @param repository     The repository identifier (e.g. "org/repo")
 * @param specPath       The relative path of the spec within the repository
 * @param checksum       Checksum of the ingested spec content
 * @param timestamp      ISO-8601 timestamp of when the ingestion occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record SpecIngestedEvent(
        UUID eventId,
        UUID specId,
        String commitSha,
        String repository,
        String specPath,
        String checksum,
        Instant timestamp,
        String idempotencyKey)
        implements DomainEvent<SpecIngestedEvent.Payload> {

    public SpecIngestedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(specId, "specId must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(checksum, "checksum must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "SpecIngested";
    }

    @Override
    public String getSource() {
        return "contract-ingestion";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Payload getPayload() {
        return new Payload(specId, commitSha, repository, specPath, checksum);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a SpecIngested event.
     */
    public record Payload(UUID specId, String commitSha, String repository, String specPath, String checksum) {}
}
