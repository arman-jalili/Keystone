// Canonical Reference: .pi/architecture/modules/dashboard.md#error-handling
// Implements: Exception for missing dashboard data
package com.keystone.dashboard.domain.exception;

/**
 * Exception thrown when requested dashboard data cannot be found.
 *
 * <p>This is an expected business exception for missing or not-yet-computed
 * data. Callers should respond with a 404 Not Found.
 */
public class DashboardDataNotFoundException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    /**
     * Constructs a new exception with a detail message.
     *
     * @param message    Human-readable description
     * @param entityType The type of entity that was not found
     * @param entityId   The identifier that was looked up
     */
    public DashboardDataNotFoundException(String message, String entityType, String entityId) {
        super(message);
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
