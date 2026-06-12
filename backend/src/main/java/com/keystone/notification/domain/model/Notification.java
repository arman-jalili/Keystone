package com.keystone.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a single notification delivery attempt.
 *
 * <p>Records what was sent, through which channel, and with what result.
 * Immutable once created — a retry produces a new Notification instance.
 *
 * @param id          Unique identifier for this notification record
 * @param channelName The name of the channel that handled delivery
 * @param channelId   The channel-specific identifier (e.g. GitHub commit SHA, email recipient)
 * @param status      Current delivery status
 * @param message     Human-readable result or error description
 * @param payloadType The type of payload that was delivered
 * @param createdAt   When this notification was created
 */
public record Notification(
        UUID id,
        String channelName,
        String channelId,
        NotificationStatus status,
        String message,
        String payloadType,
        Instant createdAt) {
    public Notification {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(channelName, "channelName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(payloadType, "payloadType must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Returns true if this notification was successfully delivered.
     */
    public boolean isDelivered() {
        return status == NotificationStatus.DELIVERED;
    }

    /**
     * Returns true if this notification delivery failed.
     */
    public boolean isFailed() {
        return status == NotificationStatus.FAILED;
    }

    /**
     * Returns a human-readable summary of this notification.
     */
    public String summary() {
        return "Notification[" + id + "] channel=" + channelName + " status=" + status + " payloadType=" + payloadType;
    }
}
