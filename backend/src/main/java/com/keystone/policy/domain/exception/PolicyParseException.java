package com.keystone.policy.domain.exception;

import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when a policy DSL expression or policy file fails to parse.
 *
 * <p>Carries a list of {@link ParseError} details so the API layer
 * can return a structured 422 Unprocessable Entity response.
 */
public class PolicyParseException extends RuntimeException {

    private final List<ParseError> errors;

    public PolicyParseException(String message, List<ParseError> errors) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    /**
     * A single parse error with line/column position and a human-readable message.
     *
     * @param line     The line number where the error occurred (1-indexed)
     * @param column   The column number where the error occurred (1-indexed)
     * @param message  A human-readable description of the parse error
     */
    public record ParseError(int line, int column, String message) {
        public ParseError {
            if (line < 1) throw new IllegalArgumentException("line must be >= 1");
            if (column < 1) throw new IllegalArgumentException("column must be >= 1");
            Objects.requireNonNull(message, "message must not be null");
        }
    }
}
