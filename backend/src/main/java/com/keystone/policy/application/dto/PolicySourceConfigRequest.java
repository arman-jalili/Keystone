package com.keystone.policy.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Input DTO for configuring a policy source.
 *
 * <p>A policy source defines where policies are loaded from.
 * The primary source type is Git, which loads policies from a
 * repository's policy directory.
 *
 * @param sourceId   A unique identifier for this source (e.g. "org-policies")
 * @param sourceType The type of source ("git", "local", "http")
 * @param location   The source location (Git URL, file path, or HTTP URL)
 * @param branch     The Git branch to use (only for "git" source type)
 * @param policyPath The directory path within the source where policy files are stored
 * @param authToken  Optional authentication token (GitHub PAT, etc.)
 * @param enabled    Whether this source is enabled for sync
 */
public record PolicySourceConfigRequest(
        @NotBlank(message = "sourceId is required")
                @Size(max = 256, message = "sourceId must not exceed 256 characters")
                String sourceId,
        @NotBlank(message = "sourceType is required") String sourceType,
        @NotBlank(message = "location is required")
                @Size(max = 2048, message = "location must not exceed 2048 characters")
                String location,
        @Size(max = 256, message = "branch must not exceed 256 characters") String branch,
        @NotBlank(message = "policyPath is required")
                @Size(max = 1024, message = "policyPath must not exceed 1024 characters")
                String policyPath,
        @Size(max = 2048, message = "authToken must not exceed 2048 characters") String authToken,
        boolean enabled) {
    public PolicySourceConfigRequest {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(policyPath, "policyPath must not be null");
    }
}
