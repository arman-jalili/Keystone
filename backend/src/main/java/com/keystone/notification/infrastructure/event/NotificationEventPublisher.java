// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.infrastructure.event;

import com.keystone.notification.domain.event.NotificationDeliveryFailedEvent;
import com.keystone.notification.domain.event.NotificationSentEvent;

/**
 * Outbound port for publishing notification-related domain events.
 *
 * <p>Per the event-driven architecture, notification events are published
 * via the event bus. The initial implementation uses Spring's
 * {@code ApplicationEventPublisher} (in-process), with the option
 * to swap for Redis Streams or Kafka later.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Publish events synchronously or asynchronously (configurable)</li>
 *   <li>Ensure at-least-once delivery semantics</li>
 *   <li>Not swallow exceptions — errors should propagate to the caller</li>
 * </ul>
 */
public interface NotificationEventPublisher {

    /**
     * Publishes a {@link NotificationSentEvent} when a notification has
     * been successfully delivered through a channel.
     *
     * <p>Consumers: Dashboard (notification history), audit logging.
     *
     * @param event the event to publish
     */
    void notificationSent(NotificationSentEvent event);

    /**
     * Publishes a {@link NotificationDeliveryFailedEvent} when a notification
     * delivery fails after all retry attempts are exhausted.
     *
     * <p>Consumers: Alerting, dashboard (failure tracking), retry scheduler.
     *
     * @param event the event to publish
     */
    void notificationDeliveryFailed(NotificationDeliveryFailedEvent event);
}
