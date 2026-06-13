// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;

/**
 * Standard error response DTO returned on analysis failures.
 *
 * @param code    A machine-readable error code
 * @param message A human-readable error description
 * @param details Optional list of error details
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, List<ErrorDetail> details) {
    public ErrorResponse {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record ErrorDetail(String field, String message) {
        public ErrorDetail {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }
}
