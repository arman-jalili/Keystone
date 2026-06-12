package com.keystone.policy.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;

/**
 * Standard error response DTO returned on policy engine failures.
 *
 * <p>Follows the same error envelope pattern as other Keystone modules.
 *
 * @param code    A machine-readable error code (e.g. "POLICY_PARSE_ERROR", "SYNC_FAILED")
 * @param message A human-readable error description
 * @param details Optional list of field-level error details
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

    /**
     * A single field-level error detail.
     *
     * @param field   The field path that caused the error (e.g. "dslExpression")
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
