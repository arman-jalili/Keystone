package com.keystone.graph.domain.event;

import com.keystone.graph.domain.model.ImpactAnalysisResult;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when impact analysis has been computed.
 *
 * <p>Published by {@link com.keystone.graph.domain.service.ImpactAnalyzer} after
 * computing the blast radius of a breaking change via BFS traversal.
 *
 * <p>Consumers: Notification Engine (alerts affected service teams),
 * Dashboard (updates impact view),
 * Audit (records impact analysis event).
 *
 * @param eventId          Unique identifier for this event occurrence
 * @param reportId         The ID of the breaking change report that triggered the analysis
 * @param affectedServices The list of affected downstream services
 * @param totalAffected    Total number of affected services
 * @param timestamp        ISO-8601 timestamp of when the analysis completed
 * @param idempotencyKey   Deduplication key for this event
 */
public record DownstreamImpactComputedEvent(
        UUID eventId,
        UUID reportId,
        List<ImpactAnalysisResult.AffectedService> affectedServices,
        int totalAffected,
        Instant timestamp,
        String idempotencyKey)
        implements DomainEvent<DownstreamImpactComputedEvent.Payload> {

    public DownstreamImpactComputedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(affectedServices, "affectedServices must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "DownstreamImpactComputed";
    }

    @Override
    public String getSource() {
        return "dependency-graph";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Payload getPayload() {
        return new Payload(reportId, affectedServices, totalAffected);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a DownstreamImpactComputed event.
     */
    public record Payload(
            UUID reportId, List<ImpactAnalysisResult.AffectedService> affectedServices, int totalAffected) {}
}
