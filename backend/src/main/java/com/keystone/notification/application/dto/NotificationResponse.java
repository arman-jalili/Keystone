package com.keystone.notification.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Response DTO returned when a notification dispatch has been completed.
 *
 * @param notificationId Unique identifier for this notification record
 * @param channelName    The channel that handled delivery
 * @param channelId      The channel-specific identifier
 * @param status         The delivery status (PENDING, DELIVERED, FAILED, RETRYING)
 * @param message        Human-readable result or error description
 * @param payloadType    The type of payload that was delivered
 * @param createdAt      ISO-8601 timestamp of when the notification was created
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        UUID notificationId,
        String channelName,
        String channelId,
        String status,
        String message,
        String payloadType,
        Instant createdAt) {
    public NotificationResponse {
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        Objects.requireNonNull(channelName, "channelName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(payloadType, "payloadType must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Factory method to create a NotificationResponse from a domain {@link Notification}.
     */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.channelName(),
                notification.channelId(),
                notification.status().name(),
                notification.message(),
                notification.payloadType(),
                notification.createdAt());
    }

    /**
     * Returns true if the notification was successfully delivered.
     */
    public boolean isDelivered() {
        return NotificationStatus.DELIVERED.name().equals(status);
    }

    /**
     * Returns true if the notification delivery failed.
     */
    public boolean isFailed() {
        return NotificationStatus.FAILED.name().equals(status);
    }
}
