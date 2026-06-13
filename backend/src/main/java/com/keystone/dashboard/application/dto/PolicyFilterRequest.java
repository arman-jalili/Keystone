package com.keystone.dashboard.application.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for filtering policies in the Policy UI.
 *
 * <p>All fields are optional — when omitted, the server returns all policies.
 *
 * @param status   Filter by lifecycle status (ACTIVE, INACTIVE, DRAFT, ARCHIVED, ERROR)
 * @param severity Filter by severity (CRITICAL, HIGH, MEDIUM, LOW, INFO)
 * @param search   Free-text search across policy name and description
 * @param page     Zero-based page number for pagination
 * @param size     Page size for pagination
 */
public record PolicyFilterRequest(
        @Pattern(
                        regexp = "ACTIVE|INACTIVE|DRAFT|ARCHIVED|ERROR",
                        message = "status must be one of: ACTIVE, INACTIVE, DRAFT, ARCHIVED, ERROR")
                String status,
        @Pattern(
                        regexp = "CRITICAL|HIGH|MEDIUM|LOW|INFO",
                        message = "severity must be one of: CRITICAL, HIGH, MEDIUM, LOW, INFO")
                String severity,
        String search,
        int page,
        int size) {

    /** Default constructor with pagination defaults. */
    public PolicyFilterRequest() {
        this(null, null, null, 0, 20);
    }

    public PolicyFilterRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
