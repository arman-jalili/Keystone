// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.domain.exception;

import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when an OpenAPI spec fails validation or parsing.
 *
 * <p>Carries a list of {@link ValidationError} details so the API layer
 * can return a structured 422 Unprocessable Entity response.
 */
public class SpecParseException extends RuntimeException {

    private final List<ValidationError> details;

    public SpecParseException(String message, List<ValidationError> details) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.details = List.copyOf(Objects.requireNonNull(details, "details must not be null"));
    }

    public List<ValidationError> getDetails() {
        return details;
    }

    /**
     * A single validation error with the field path and a human-readable message.
     */
    public record ValidationError(String field, String message) {
        public ValidationError {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }
}
