// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Domain event published when a health score is recalculated
package com.keystone.dashboard.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a health score has been recalculated.
 *
 * <p>Consumers: Dashboard (caches the latest score), webhook notification
 * (alert if score drops below threshold), audit logging.
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param entityType     The type of entity that was scored
 * @param entityId       The identifier of the entity within its type namespace
 * @param previousScore  The score value before recalculation
 * @param newScore       The score value after recalculation
 * @param timestamp      ISO-8601 timestamp of when the event occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record HealthScoreRecalculatedEvent(
        UUID eventId,
        String entityType,
        String entityId,
        double previousScore,
        double newScore,
        Instant timestamp,
        String idempotencyKey)
        implements DomainEvent<HealthScoreRecalculatedEvent.Payload> {

    public HealthScoreRecalculatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "HealthScoreRecalculated";
    }

    @Override
    public String getSource() {
        return "dashboard-engine";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Payload getPayload() {
        return new Payload(entityType, entityId, previousScore, newScore);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a HealthScoreRecalculated event.
     */
    public record Payload(String entityType, String entityId, double previousScore, double newScore) {}
}
