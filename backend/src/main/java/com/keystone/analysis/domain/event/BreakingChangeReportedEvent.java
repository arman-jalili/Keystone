package com.keystone.analysis.domain.event;

import com.keystone.analysis.domain.model.BreakingChangeReport;
import com.keystone.analysis.domain.model.Change;
import com.keystone.analysis.domain.model.Verdict;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a breaking change analysis has completed.
 *
 * <p>Consumers (e.g. Policy Engine, Dashboard, Notification Engine) subscribe
 * to this event to trigger downstream workflows.
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param reportId       The ID of the completed {@link BreakingChangeReport}
 * @param repository     The repository that was analysed
 * @param specPath       The spec path that was analysed
 * @param verdict        The overall analysis verdict
 * @param changes        The list of all detected changes
 * @param timestamp      ISO-8601 timestamp of when the analysis completed
 * @param idempotencyKey Deduplication key for this event
 */
public record BreakingChangeReportedEvent(
    UUID eventId,
    UUID reportId,
    String repository,
    String specPath,
    Verdict verdict,
    List<Change> changes,
    Instant timestamp,
    String idempotencyKey
) implements DomainEvent<BreakingChangeReportedEvent.Payload> {

    public BreakingChangeReportedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(changes, "changes must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() { return eventId; }

    @Override
    public String getEventType() { return "BreakingChangeReported"; }

    @Override
    public String getSource() { return "breaking-change-analysis"; }

    @Override
    public Instant getTimestamp() { return timestamp; }

    @Override
    public Payload getPayload() {
        return new Payload(reportId, repository, specPath, verdict, changes);
    }

    @Override
    public String getIdempotencyKey() { return idempotencyKey; }

    /**
     * The data payload carried by a BreakingChangeReported event.
     */
    public record Payload(
        UUID reportId,
        String repository,
        String specPath,
        Verdict verdict,
        List<Change> changes
    ) {}
}
