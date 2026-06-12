package com.keystone.breaking.domain.event;

import com.keystone.breaking.domain.model.BreakingChange;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when breaking changes are detected during analysis.
 *
 * <p>Unlike {@link AnalysisCompletedEvent} which is published for every
 * completed analysis, this event is only published when at least one
 * {@link BreakingChange.Severity#BREAKING} change is found. Consumers
 * such as the notification engine or CI pipeline integrations subscribe
 * to this event to trigger alerts or block merges.
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param reportId       The ID of the {@link com.keystone.breaking.domain.model.DiffReport}
 * @param repository     The repository that was analysed
 * @param specPath       The spec path that was analysed
 * @param breakingCount  Number of BREAKING-severity changes
 * @param changes        Summary list of the breaking changes detected
 * @param timestamp      ISO-8601 timestamp of when the event occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record BreakingChangesFoundEvent(
    UUID eventId,
    UUID reportId,
    String repository,
    String specPath,
    int breakingCount,
    List<BreakingChange> changes,
    Instant timestamp,
    String idempotencyKey
) implements DomainEvent<BreakingChangesFoundEvent.Payload> {

    public BreakingChangesFoundEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(changes, "changes must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "BreakingChangesFound";
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
        return new Payload(reportId, repository, specPath, breakingCount, changes);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a BreakingChangesFound event.
     */
    public record Payload(
        UUID reportId,
        String repository,
        String specPath,
        int breakingCount,
        List<BreakingChange> changes
    ) {}
}
