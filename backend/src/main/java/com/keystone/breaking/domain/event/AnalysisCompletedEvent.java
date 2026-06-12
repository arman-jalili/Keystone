package com.keystone.breaking.domain.event;

import com.keystone.breaking.domain.model.DiffReport;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a diff analysis has completed.
 *
 * <p>Consumers (e.g. dashboard, notification engine) subscribe to this
 * event to trigger downstream workflows such as policy evaluation or
 * user notification.
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param reportId       The ID of the completed {@link DiffReport}
 * @param repository     The repository that was analysed
 * @param specPath       The spec path that was analysed
 * @param breakingFound  Whether breaking changes were detected
 * @param totalChanges   Total number of changes detected across all severity levels
 * @param timestamp      ISO-8601 timestamp of when the analysis completed
 * @param idempotencyKey Deduplication key for this event
 */
public record AnalysisCompletedEvent(
    UUID eventId,
    UUID reportId,
    String repository,
    String specPath,
    boolean breakingFound,
    int totalChanges,
    Instant timestamp,
    String idempotencyKey
) implements DomainEvent<AnalysisCompletedEvent.Payload> {

    public AnalysisCompletedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "AnalysisCompleted";
    }

    @Override
    public String getSource() {
        return "breaking-change-analysis";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Payload getPayload() {
        return new Payload(reportId, repository, specPath, breakingFound, totalChanges);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by an AnalysisCompleted event.
     */
    public record Payload(
        UUID reportId,
        String repository,
        String specPath,
        boolean breakingFound,
        int totalChanges
    ) {}
}
