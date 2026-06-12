package com.keystone.notification.domain.channel;

import com.keystone.notification.domain.model.CiStatusPayload;
import com.keystone.notification.domain.model.Notification;

/**
 * Specialized notification channel for posting commit status updates
 * to GitHub and GitLab commit status APIs.
 *
 * <p>This channel:
 * <ul>
 *   <li>Builds a {@link CiStatusPayload} from incoming events</li>
 *   <li>Posts to the Git provider's commit status API via REST</li>
 *   <li>Wraps API calls in a Resilience4j circuit breaker</li>
 *   <li>Supports configurable timeouts (default 2s)</li>
 * </ul>
 *
 * <p>The CI status context is set to {@code "keystone/governance"} by default.
 * States posted: "pending", "success", "failure", "error".
 *
 * @see NotificationChannel
 * @see CiStatusPayload
 */
public interface CiStatusChannel extends NotificationChannel {

    /**
     * Returns the CI context label used for all status updates.
     *
     * @return the context string (e.g. "keystone/governance")
     */
    String getContext();

    /**
     * Posts a commit status update using the given payload.
     *
     * <p>This method bypasses event extraction — the caller provides
     * a pre-built {@link CiStatusPayload} directly. Useful for
     * programmatic status updates outside the normal event flow.
     *
     * @param payload the pre-built CI status payload
     * @return a Notification record capturing the delivery result
     */
    Notification postStatus(CiStatusPayload payload);

    /**
     * Extracts a {@link CiStatusPayload} from an arbitrary event object.
     *
     * <p>Used internally by {@link #send(Object)} to convert incoming
     * domain events (e.g. {@code PolicyEvaluatedEvent}) into CI status
     * payloads. Implementations determine the state mapping based on
     * the event type and content.
     *
     * @param event the incoming event object
     * @return the extracted CI status payload
     * @throws IllegalArgumentException if the event type is not supported
     */
    CiStatusPayload extractPayload(Object event);
}
