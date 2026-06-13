// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Exception for health score computation failures
package com.keystone.dashboard.domain.exception;

/**
 * Exception thrown when health score computation fails.
 *
 * <p>This is an unrecoverable computation error — the caller should
 * report the failure and return a fallback status rather than retrying.
 * Common causes include missing input data or inconsistent state.
 */
public class HealthScoreComputationException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    /**
     * Constructs a new exception with a detail message.
     *
     * @param message  Human-readable description of the failure
     * @param entityType The type of entity being scored
     * @param entityId   The identifier of the entity
     */
    public HealthScoreComputationException(String message, String entityType, String entityId) {
        super(message);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    /**
     * Constructs a new exception with a detail message and cause.
     *
     * @param message  Human-readable description of the failure
     * @param cause    The root cause of the computation failure
     * @param entityType The type of entity being scored
     * @param entityId   The identifier of the entity
     */
    public HealthScoreComputationException(String message, Throwable cause, String entityType, String entityId) {
        super(message, cause);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
