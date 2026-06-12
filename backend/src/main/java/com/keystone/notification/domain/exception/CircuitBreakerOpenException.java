package com.keystone.notification.domain.exception;

/**
 * Exception thrown when a notification channel's circuit breaker is open
 * and the channel cannot accept delivery requests.
 *
 * <p>The circuit breaker opens when the external service returns too many
 * errors or timeouts. After a cooldown period (default 30s), the circuit
 * transitions to half-open and allows a single probe request.
 *
 * <p>Dispatchers should catch this exception, log a warning, and move on
 * to the next channel. The failed event will be retried on the next
 * relevant domain event.
 *
 * @see com.keystone.notification.domain.channel.NotificationChannel#isAvailable()
 */
public class CircuitBreakerOpenException extends RuntimeException {

    private final String channel;

    /**
     * Creates a circuit breaker exception for the given channel.
     *
     * @param channel the channel name whose circuit breaker is open
     */
    public CircuitBreakerOpenException(String channel) {
        super("Circuit breaker open for channel: " + channel);
        this.channel = channel;
    }

    /**
     * Creates a circuit breaker exception with a root cause.
     *
     * @param channel the channel name whose circuit breaker is open
     * @param cause   the root cause that triggered the circuit breaker
     */
    public CircuitBreakerOpenException(String channel, Throwable cause) {
        super("Circuit breaker open for channel: " + channel, cause);
        this.channel = channel;
    }

    /**
     * Returns the channel name whose circuit breaker is open.
     */
    public String getChannel() {
        return channel;
    }
}
