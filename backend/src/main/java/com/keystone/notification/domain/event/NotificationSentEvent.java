package com.keystone.notification.domain.event;

import com.keystone.notification.domain.model.Notification;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a notification has been successfully delivered
 * through a channel.
 *
 * <p>Consumers (e.g. dashboard, audit log) subscribe to this event to track
 * notification history. Per ADR-004, this event is stored as an immutable
 * audit event.
 *
 * @param eventId    Unique identifier for this event occurrence
 * @param notification The delivered notification record
 * @param timestamp  ISO-8601 timestamp of when the delivery occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record NotificationSentEvent(UUID eventId, Notification notification, Instant timestamp, String idempotencyKey)
        implements NotificationDomainEvent<NotificationSentEvent.Payload> {

    public NotificationSentEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(notification, "notification must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public String getEventType() {
        return "NotificationSent";
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
        return new Payload(
                notification.id(),
                notification.channelName(),
                notification.channelId(),
                notification.status().name(),
                notification.payloadType());
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a NotificationSent event.
     *
     * @param notificationId The UUID of the delivered notification
     * @param channelName    The channel that delivered the notification
     * @param channelId      The channel-specific identifier
     * @param status         The delivery status
     * @param payloadType    The type of payload that was delivered
     */
    public record Payload(
            UUID notificationId, String channelName, String channelId, String status, String payloadType) {}
}
