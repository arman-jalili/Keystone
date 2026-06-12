package com.keystone.analysis.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO representing a single change in API responses.
 *
 * <p>Provides a serializable representation of the domain
 * {@link com.keystone.analysis.domain.model.Change} value object.
 *
 * @param id           Unique identifier for this finding
 * @param severity     Severity level: BREAKING, NON_BREAKING, ADDITIVE, or DEPRECATION
 * @param path         JSON Pointer or dot-notation path to the changed element
 * @param oldValue     The value in the base specification
 * @param newValue     The value in the new specification
 * @param message      Human-readable description of the change
 * @param detectorName Name of the detector that found this change
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChangeDetail(
        UUID id, String severity, String path, String oldValue, String newValue, String message, String detectorName) {
    public ChangeDetail {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(detectorName, "detectorName must not be null");
    }
}
