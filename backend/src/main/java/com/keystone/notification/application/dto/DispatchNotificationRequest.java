package com.keystone.notification.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Input DTO for requesting notification dispatch through specific channels.
 *
 * <p>Used by the REST API to programmatically trigger notifications
 * outside of the event-driven flow (e.g. manual re-dispatch, testing).
 *
 * @param eventType   The type of event to dispatch (e.g. "PolicyEvaluated", "ExemptionGranted")
 * @param eventPayload The serialized event payload as a JSON string
 * @param channelName Optional target channel name. If omitted, dispatches to all channels
 * @param idempotencyKey Optional idempotency key for deduplication
 */
public record DispatchNotificationRequest(
        @NotBlank(message = "eventType is required")
                @Size(max = 128, message = "eventType must not exceed 128 characters")
                String eventType,
        @NotBlank(message = "eventPayload is required") String eventPayload,
        @Size(max = 64, message = "channelName must not exceed 64 characters") String channelName,
        @Size(max = 256, message = "idempotencyKey must not exceed 256 characters") String idempotencyKey) {
    public DispatchNotificationRequest {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(eventPayload, "eventPayload must not be null");
    }

    /**
     * Returns true if a specific target channel was requested.
     */
    public boolean hasTargetChannel() {
        return channelName != null && !channelName.isBlank();
    }

    /**
     * Returns true if an idempotency key was provided.
     */
    public boolean hasIdempotencyKey() {
        return idempotencyKey != null && !idempotencyKey.isBlank();
    }
}
