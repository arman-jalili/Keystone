package com.keystone.breaking.domain.service;

import com.keystone.breaking.domain.detector.BreakingChangeDetector;
import com.keystone.breaking.domain.exception.DiffAnalysisException;
import com.keystone.breaking.domain.model.BaseVersion;
import com.keystone.breaking.domain.model.DiffReport;

/**
 * Domain service that orchestrates the full diff analysis pipeline.
 *
 * <p>Coordinates the following workflow:
 * <ol>
 *   <li>Resolve the base version via {@link BaseVersionResolver}</li>
 *   <li>Retrieve the base specification content</li>
 *   <li>Execute all registered {@link BreakingChangeDetector} instances</li>
 *   <li>Aggregate results into a {@link DiffReport}</li>
 *   <li>Publish analysis events via the event publisher infrastructure</li>
 * </ol>
 *
 * <p>Implementations must support registration of both built-in and
 * custom detectors. The set of detectors used for a given analysis
 * may be filtered by the caller.
 */
public interface DiffOrchestrator {

    /**
     * Runs the full diff analysis pipeline for the given specification.
     *
     * <p>This is the primary entry point for triggering breaking change
     * analysis. It resolves the base version, fetches the base spec,
     * runs all detectors, and produces a {@link DiffReport}.
     *
     * @param repository    the repository identifier (e.g. "org/repo")
     * @param specPath      the relative spec path within the repository
     * @param newSpecCommit the git commit SHA of the new spec version
     * @return the completed diff report
     * @throws DiffAnalysisException if any step in the pipeline fails
     */
    DiffReport analyze(String repository, String specPath, String newSpecCommit)
            throws DiffAnalysisException;

    /**
     * Runs diff analysis using an explicitly provided base version,
     * bypassing the resolver.
     *
     * <p>Useful for manual re-analysis or when the caller knows the
     * exact base version to compare against.
     *
     * @param repository    the repository identifier
     * @param specPath      the relative spec path
     * @param newSpecCommit the new spec commit SHA
     * @param baseVersion   the explicitly resolved base version
     * @return the completed diff report
     * @throws DiffAnalysisException if analysis fails
     */
    DiffReport analyzeWithBase(String repository, String specPath, String newSpecCommit,
                               BaseVersion baseVersion) throws DiffAnalysisException;

    /**
     * Registers a breaking change detector.
     *
     * <p>Detectors are executed in registration order. If a detector
     * with the same ID is already registered, it is replaced.
     *
     * @param detector the detector to register
     */
    void registerDetector(BreakingChangeDetector detector);
}
