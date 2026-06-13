// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for all domain events in the Notification bounded context.
 *
 * <p>Per the event-driven architecture, every domain event carries:
 * <ul>
 *   <li>An event ID for idempotent processing</li>
 *   <li>A source identifier for the publishing context</li>
 *   <li>A timestamp in ISO-8601 format</li>
 *   <li>An idempotency key for deduplication</li>
 * </ul>
 *
 * @param <T> The type of the event payload
 */
public interface NotificationDomainEvent<T> {

    /**
     * Unique identifier for this event occurrence.
     */
    UUID getEventId();

    /**
     * The logical event type name (e.g. "NotificationSent", "NotificationDeliveryFailed").
     */
    String getEventType();

    /**
     * Source context that published this event (e.g. "notification-engine").
     */
    String getSource();

    /**
     * ISO-8601 timestamp of when the event occurred.
     */
    Instant getTimestamp();

    /**
     * The payload carrying event-specific data.
     */
    T getPayload();

    /**
     * The idempotency key for deduplication, if applicable.
     */
    String getIdempotencyKey();
}
