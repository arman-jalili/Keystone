// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.domain.exception;

/**
 * Exception thrown when a notification channel fails to deliver after all
 * retry attempts are exhausted.
 *
 * <p>This exception is not thrown by the channel itself (channels return
 * {@link com.keystone.notification.domain.model.Notification} with FAILED status),
 * but by the dispatcher when a notification cannot be delivered through
 * any available channel.
 */
public class NotificationDeliveryException extends RuntimeException {

    private final String channel;
    private final int retryAttempt;

    /**
     * Creates a delivery exception with the given details.
     *
     * @param channel     the channel name that failed
     * @param message     the error description
     * @param retryAttempt the number of retry attempts made
     */
    public NotificationDeliveryException(String channel, String message, int retryAttempt) {
        super("Channel " + channel + " failed after " + retryAttempt + " retries: " + message);
        this.channel = channel;
        this.retryAttempt = retryAttempt;
    }

    /**
     * Creates a delivery exception with a cause.
     *
     * @param channel     the channel name that failed
     * @param message     the error description
     * @param retryAttempt the number of retry attempts made
     * @param cause       the root cause
     */
    public NotificationDeliveryException(String channel, String message, int retryAttempt, Throwable cause) {
        super("Channel " + channel + " failed after " + retryAttempt + " retries: " + message, cause);
        this.channel = channel;
        this.retryAttempt = retryAttempt;
    }

    /**
     * Returns the channel name that failed.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Returns the number of retry attempts made.
     */
    public int getRetryAttempt() {
        return retryAttempt;
    }
}
