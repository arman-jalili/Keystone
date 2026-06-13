// Canonical Reference: .pi/architecture/modules/dependency-graph.md
// Module: dependency-graph
package com.keystone.graph.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;

/**
 * Standard error response DTO returned on dependency graph failures.
 *
 * <p>Follows the same error envelope pattern as other bounded contexts.
 *
 * @param code     A machine-readable error code (e.g. "UNKNOWN_SERVICE", "DUPLICATE_DEPENDENCY")
 * @param message  A human-readable error description
 * @param details  Optional list of field-level validation errors
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @NotBlank(message = "code is required") String code,
        @NotBlank(message = "message is required") String message,
        List<ErrorDetail> details) {
    public ErrorResponse {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    /**
     * A single field-level error detail.
     *
     * @param field   The field path that caused the error
     * @param message A human-readable description of the error
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ErrorDetail(String field, String message) {
        public ErrorDetail {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }
}
