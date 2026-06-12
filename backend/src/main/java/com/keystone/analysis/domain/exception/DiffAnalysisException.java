package com.keystone.analysis.domain.exception;

import java.util.Objects;

/**
 * Exception thrown when the diff analysis pipeline fails.
 *
 * <p>Per the architecture, individual detector failures should be logged
 * and the detector should be skipped while remaining detectors continue.
 * This exception is reserved for pipeline-level failures where the
 * analysis cannot proceed at all.
 */
public class DiffAnalysisException extends RuntimeException {

    private final String repository;
    private final String specPath;
    private final String stage;

    public DiffAnalysisException(String message, String repository, String specPath, String stage) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
    }

    public DiffAnalysisException(String message, String repository, String specPath,
                                  String stage, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
    }

    public String getRepository() { return repository; }
    public String getSpecPath() { return specPath; }
    public String getStage() { return stage; }

    @Override
    public String toString() {
        return "DiffAnalysisException{repository='" + repository + "', specPath='" + specPath
               + "', stage='" + stage + "'}";
    }
}
