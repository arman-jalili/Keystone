package com.keystone.ingestion.domain.exception;

import java.util.Objects;
import java.util.UUID;

/**
 * Exception thrown when a duplicate spec is detected.
 *
 * <p>Rather than an error, this is an informational signal that the spec
 * has already been processed. The API layer should return 200 OK with the
 * existing event ID, following the idempotency contract (ADR-007).
 */
public class DuplicateSpecException extends RuntimeException {

    private final UUID existingEventId;

    public DuplicateSpecException(UUID existingEventId) {
        super("Spec already processed: " + Objects.requireNonNull(existingEventId, "existingEventId must not be null"));
        this.existingEventId = existingEventId;
    }

    public UUID getExistingEventId() {
        return existingEventId;
    }
}
