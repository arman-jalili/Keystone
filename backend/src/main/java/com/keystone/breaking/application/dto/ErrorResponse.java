package com.keystone.breaking.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Standard error response DTO returned on analysis failures.
 *
 * <p>Follows a consistent error envelope across all breaking-change-analysis endpoints.
 *
 * @param code    A machine-readable error code (e.g. "DIFF_ANALYSIS_ERROR", "VERSION_RESOLUTION_ERROR")
 * @param message A human-readable error description
 * @param details Optional list of field-level or step-level error details
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    String code,
    String message,
    List<ErrorDetail> details
) {
    public ErrorResponse {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    /**
     * A single field-level or step-level error detail.
     *
     * @param field   The field or analysis step that caused the error
     * @param message A human-readable description of the error
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ErrorDetail(
        String field,
        String message
    ) {
        public ErrorDetail {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }
}
