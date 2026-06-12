package com.keystone.graph.domain.exception;

/**
 * Thrown when a duplicate dependency edge is detected during registration.
 *
 * <p>Per the data model, the combination of (producer, consumer, specPath)
 * must be unique. However, duplicate registration is treated as idempotent —
 * the implmentation should ignore the duplicate rather than propagate this error.
 */
public class DuplicateDependencyException extends RuntimeException {

    public DuplicateDependencyException(String message) {
        super(message);
    }

    public DuplicateDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a descriptive message.
     *
     * @param producer The name of the producer service
     * @param consumer The name of the consumer service
     */
    public static DuplicateDependencyException forEdge(String producer, String consumer) {
        return new DuplicateDependencyException("Duplicate dependency: " + producer + " → " + consumer);
    }
}
