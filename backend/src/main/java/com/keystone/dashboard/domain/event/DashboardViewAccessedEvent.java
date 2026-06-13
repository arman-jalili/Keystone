package com.keystone.dashboard.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a user accesses a dashboard view.
 *
 * <p>Consumers: Analytics (usage tracking), caching layer (pre-fetch),
 * audit logging.
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param userId         The identifier of the user accessing the dashboard
 * @param viewType       The type of dashboard view accessed
 * @param timestamp      ISO-8601 timestamp of when the event occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record DashboardViewAccessedEvent(
        UUID eventId, String userId, String viewType, Instant timestamp, String idempotencyKey)
        implements DomainEvent<DashboardViewAccessedEvent.Payload> {

    public DashboardViewAccessedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(viewType, "viewType must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "DashboardViewAccessed";
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
        return new Payload(userId, viewType);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a DashboardViewAccessed event.
     */
    public record Payload(String userId, String viewType) {}
}
