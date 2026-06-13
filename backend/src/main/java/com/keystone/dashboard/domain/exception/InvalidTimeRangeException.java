// Canonical Reference: .pi/architecture/modules/dashboard.md#error-handling
// Implements: Exception for invalid time range parameters
package com.keystone.dashboard.domain.exception;

/**
 * Exception thrown when an invalid or malformed time range is provided.
 *
 * <p>Callers should catch this and respond with a 400 Bad Request.
 * Examples: end before start, range too large, unsupported range type.
 */
public class InvalidTimeRangeException extends RuntimeException {

    /**
     * Constructs a new exception with a detail message.
     *
     * @param message Human-readable description of the invalid range
     */
    public InvalidTimeRangeException(String message) {
        super(message);
    }
}
