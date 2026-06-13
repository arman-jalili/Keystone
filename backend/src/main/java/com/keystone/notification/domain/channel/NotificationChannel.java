// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.domain.channel;

import com.keystone.notification.domain.model.Notification;
import com.keystone.notification.domain.model.NotificationStatus;

/**
 * Base contract for all notification delivery channels.
 *
 * <p>Each channel type (CI status, email, Slack) implements this interface
 * to provide its own delivery logic. Channels are registered in the
 * {@link com.keystone.notification.domain.service.ChannelRegistry} and
 * invoked by {@link com.keystone.notification.application.service.NotificationDispatcher}.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Handle delivery with configurable timeout and retry logic</li>
 *   <li>Wrap external calls in a circuit breaker (Resilience4j)</li>
 *   <li>Return a {@link Notification} with the appropriate status</li>
 *   <li>Not throw exceptions — wrap failures in {@link Notification} with status FAILED</li>
 * </ul>
 */
public interface NotificationChannel {

    /**
     * Returns the unique name identifier for this channel.
     *
     * <p>Used by {@link com.keystone.notification.domain.service.ChannelRegistry}
     * to look up channels by name.
     *
     * @return the channel name (e.g. "CI_STATUS", "EMAIL", "SLACK")
     */
    String getName();

    /**
     * Sends a notification through this channel.
     *
     * <p>The channel is responsible for serializing the event payload
     * into the format expected by the external system. Delivery is
     * synchronous — the caller receives a {@link Notification} with
     * the final delivery status.
     *
     * <p>Failed deliveries should set status to {@link NotificationStatus#FAILED}
     * with a descriptive message. The dispatcher handles retry logic.
     *
     * @param event the event object to deliver
     * @return a Notification record capturing the delivery result
     */
    Notification send(Object event);

    /**
     * Returns true if this channel is currently available for delivery.
     *
     * <p>A channel may be unavailable if its circuit breaker is open,
     * the external service is unreachable, or it has been administratively
     * disabled.
     *
     * @return true if the channel can accept delivery requests
     */
    boolean isAvailable();
}
