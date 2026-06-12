package com.keystone.breaking.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keystone.breaking.domain.model.BreakingChange;

import java.util.Objects;
import java.util.UUID;

/**
 * DTO representing a single breaking change in API responses.
 *
 * <p>Provides a serializable representation of the domain
 * {@link BreakingChange} value object, suitable for JSON responses.
 *
 * @param id          Unique identifier for this finding
 * @param type        Machine-readable change type code
 * @param severity    Severity level: BREAKING, WARNING, or INFO
 * @param path        JSON Pointer or dot-notation path to the changed element
 * @param oldValue    The value in the base specification
 * @param newValue    The value in the new specification
 * @param description Human-readable description of the change
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BreakingChangeDetail(
    UUID id,
    String type,
    String severity,
    String path,
    String oldValue,
    String newValue,
    String description
) {
    public BreakingChangeDetail {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
