package com.keystone.breaking.domain.exception;

import java.util.Objects;

/**
 * Exception thrown when the base version cannot be resolved.
 *
 * <p>This is a non-recoverable exception for the current analysis request.
 * Causes include: the spec has never been ingested before and no base
 * branch is configured, the base branch does not contain the spec,
 * or the version resolution service is unavailable.
 */
public class VersionResolutionException extends RuntimeException {

    private final String repository;
    private final String specPath;
    private final String reason;

    public VersionResolutionException(String message, String repository, String specPath, String reason) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public VersionResolutionException(String message, String repository, String specPath,
                                       String reason, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public String getRepository() {
        return repository;
    }

    public String getSpecPath() {
        return specPath;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "VersionResolutionException{repository='" + repository + "', specPath='" + specPath
               + "', reason='" + reason + "'}";
    }
}
