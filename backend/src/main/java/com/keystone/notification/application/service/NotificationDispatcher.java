// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.application.service;

import com.keystone.notification.application.dto.ChannelStatusResponse;
import com.keystone.notification.application.dto.DispatchNotificationRequest;
import com.keystone.notification.application.dto.NotificationResponse;
import com.keystone.notification.domain.exception.NotificationDeliveryException;
import java.util.List;
import java.util.UUID;

/**
 * Application service interface for dispatching notifications to registered channels.
 *
 * <p>This is the primary inbound port (driving adapter) for the notification engine.
 * The {@link com.keystone.notification.interfaces.http.NotificationController}
 * and event listeners depend on this interface.
 *
 * <p>Handles two dispatch modes:
 * <ol>
 *   <li><b>Event-driven</b> — Subscribes to domain events (e.g. {@code PolicyEvaluatedEvent},
 *       {@code ExemptionGrantedEvent}) via Spring's {@code @EventListener} and dispatches
 *       to all registered channels.</li>
 *   <li><b>Programmatic</b> — Accepts a {@link DispatchNotificationRequest} through the
 *       REST API for manual triggering or testing.</li>
 * </ol>
 *
 * <p>Dispatch behavior:
 * <ul>
 *   <li>Channels are invoked in parallel (CompletableFuture)</li>
 *   <li>Failed deliveries are retried with exponential backoff (1s, 4s, 10s)</li>
 *   <li>After 3 retries, a {@link NotificationDeliveryException} is thrown</li>
 *   <li>One channel failure does not affect other channels</li>
 * </ul>
 */
public interface NotificationDispatcher {

    /**
     * Dispatches a domain event to all registered notification channels.
     *
     * <p>Each channel receives the event and produces a {@code Notification}.
     * Results are aggregated and returned as a list. Failed channels are
     * retried according to the configured retry policy.
     *
     * @param event the domain event to dispatch (e.g. PolicyEvaluatedEvent)
     * @return list of notification responses, one per channel
     */
    List<NotificationResponse> dispatch(Object event);

    /**
     * Dispatches a notification through a specific channel by name.
     *
     * <p>Useful for targeting a single channel (e.g. re-sending a CI status
     * update without re-dispatching to email/Slack).
     *
     * @param channelName the target channel name (e.g. "CI_STATUS")
     * @param event       the event to dispatch
     * @return the notification response from the targeted channel
     * @throws NotificationDeliveryException if the channel is not found or delivery fails
     */
    NotificationResponse dispatchToChannel(String channelName, Object event) throws NotificationDeliveryException;

    /**
     * Dispatches a notification using a pre-built request DTO.
     *
     * <p>Used by the REST API for manual dispatch. The request carries
     * a serialized event payload and optional target channel.
     *
     * @param request the dispatch request with serialized payload
     * @return list of notification responses
     * @throws NotificationDeliveryException if dispatch fails
     */
    List<NotificationResponse> dispatchRequest(DispatchNotificationRequest request)
            throws NotificationDeliveryException;

    /**
     * Retrieves the delivery status for a specific notification.
     *
     * @param notificationId the UUID of the notification record
     * @return the notification response, or empty if not found
     */
    java.util.Optional<NotificationResponse> getNotificationStatus(UUID notificationId);

    /**
     * Returns the status of all registered notification channels.
     *
     * @return channel status information
     */
    ChannelStatusResponse getChannelStatus();

    /**
     * Returns all notifications, ordered by creation timestamp descending.
     * Used by the Dashboard Notifications view.
     *
     * @param limit       the maximum number of results to return
     * @param unreadFirst if true, unread notifications appear before read ones
     * @return list of notification responses
     */
    List<NotificationResponse> listNotifications(int limit, boolean unreadFirst);
}
