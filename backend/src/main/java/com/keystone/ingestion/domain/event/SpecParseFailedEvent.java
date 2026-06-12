package com.keystone.ingestion.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an OpenAPI spec fails to parse or validate.
 *
 * <p>Carries error details so consumers can log, alert, or surface
 * the failure to the user. Per ADR-004, this event is stored as an
 * immutable audit event.
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param repository     The repository identifier (e.g. "org/repo")
 * @param commitSha      The git commit SHA that failed
 * @param specPath       The relative path of the spec that failed
 * @param errors         List of validation/parse error details
 * @param rawContentExcerpt A short excerpt of the raw input that caused the failure
 * @param timestamp      ISO-8601 timestamp of when the failure occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record SpecParseFailedEvent(
    UUID eventId,
    String repository,
    String commitSha,
    String specPath,
    List<String> errors,
    String rawContentExcerpt,
    Instant timestamp,
    String idempotencyKey
) implements DomainEvent<SpecParseFailedEvent.Payload> {

    public SpecParseFailedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(commitSha, "commitSha must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(errors, "errors must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "SpecParseFailed";
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
        return new Payload(repository, commitSha, specPath, errors, rawContentExcerpt);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a SpecParseFailed event.
     */
    public record Payload(
        String repository,
        String commitSha,
        String specPath,
        List<String> errors,
        String rawContentExcerpt
    ) {}
}
