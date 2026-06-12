package com.keystone.notification.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a notification channel fails to deliver.
 *
 * <p>Published after all retry attempts are exhausted. Consumers may
 * use this event to trigger alerts, log failures, or update dashboards.
 *
 * @param eventId    Unique identifier for this event occurrence
 * @param channelName The channel that failed to deliver
 * @param eventType  The type of event that was being delivered
 * @param errorMessage The error message from the delivery failure
 * @param retryCount Number of retry attempts made before giving up
 * @param retryable  Whether the failure may be transient and worth retrying later
 * @param timestamp  ISO-8601 timestamp of when the failure occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record NotificationDeliveryFailedEvent(
        UUID eventId,
        String channelName,
        String eventType,
        String errorMessage,
        int retryCount,
        boolean retryable,
        Instant timestamp,
        String idempotencyKey)
        implements NotificationDomainEvent<NotificationDeliveryFailedEvent.Payload> {

    public NotificationDeliveryFailedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(channelName, "channelName must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public String getEventType() {
        return "NotificationDeliveryFailed";
    }

    @Override
    public String getSource() {
        return "notification-engine";
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Payload getPayload() {
        return new Payload(channelName, eventType, errorMessage, retryCount, retryable);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a NotificationDeliveryFailed event.
     */
    public record Payload(
            String channelName, String eventType, String errorMessage, int retryCount, boolean retryable) {}
}
