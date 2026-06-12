package com.keystone.analysis.domain.exception;

import java.util.Objects;

/**
 * Exception thrown when no base version can be resolved for comparison.
 *
 * <p>Per the architecture, this occurs when all three resolution layers
 * are exhausted: no explicit base ref, no previous ingested version,
 * and no version on the main branch.
 *
 * <p>Handled by {@link com.keystone.analysis.domain.service.DiffOrchestrator}
 * by producing an {@link com.keystone.analysis.domain.model.Verdict#INCONCLUSIVE} report
 * and treating all changes as additive (safe default).
 */
public class NoBaseVersionException extends RuntimeException {

    private final String repository;
    private final String specPath;

    public NoBaseVersionException(String message, String repository, String specPath) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
    }

    public NoBaseVersionException(String message, String repository, String specPath, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
    }

    public String getRepository() {
        return repository;
    }

    public String getSpecPath() {
        return specPath;
    }

    @Override
    public String toString() {
        return "NoBaseVersionException{repository='" + repository + "', specPath='" + specPath + "'}";
    }
}
