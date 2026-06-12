package com.keystone.notification.domain.model;

/**
 * Represents the delivery status of a notification.
 *
 * <ul>
 *   <li>{@link #PENDING} — Notification queued, delivery in progress</li>
 *   <li>{@link #DELIVERED} — Successfully delivered to the target channel</li>
 *   <li>{@link #FAILED} — Delivery failed after retries or circuit breaker opened</li>
 *   <li>{@link #RETRYING} — Delivery failed but will be retried</li>
 * </ul>
 */
public enum NotificationStatus {
    PENDING,
    DELIVERED,
    FAILED,
    RETRYING
}
