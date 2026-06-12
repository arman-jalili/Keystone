package com.keystone.analysis.domain.service;

import com.keystone.analysis.domain.detector.ChangeDetector;
import com.keystone.analysis.domain.exception.DiffAnalysisException;
import com.keystone.analysis.domain.model.BaseVersion;
import com.keystone.analysis.domain.model.BreakingChangeReport;

/**
 * Domain service that orchestrates the full diff analysis pipeline.
 *
 * <p>Per the architecture, coordinates the following workflow:
 * <ol>
 *   <li>Resolve the base version via {@link BaseVersionResolver} (3-layer fallback)</li>
 *   <li>Load the target specification version</li>
 *   <li>Run all registered {@link com.keystone.analysis.domain.detector.ChangeDetector} instances</li>
 *   <li>Compute a {@link com.keystone.analysis.domain.model.Verdict}</li>
 *   <li>Persist a {@link BreakingChangeReport}</li>
 *   <li>Publish a {@link com.keystone.analysis.domain.event.BreakingChangeReportedEvent}</li>
 * </ol>
 *
 * <p>This is the primary entry point for triggering breaking change analysis.
 */
public interface DiffOrchestrator {

    /**
     * Runs the full diff analysis pipeline triggered by a spec ingestion event.
     *
     * @param repository   the repository identifier (e.g. "org/repo")
     * @param specPath     the relative spec path within the repository
     * @param targetSpecId the UUID of the target SpecVersion to analyse
     * @return the completed breaking change report
     * @throws DiffAnalysisException if any step in the pipeline fails
     */
    BreakingChangeReport analyze(String repository, String specPath, java.util.UUID targetSpecId)
            throws DiffAnalysisException;

    /**
     * Runs diff analysis using an explicitly provided base version,
     * bypassing the resolver.
     *
     * @param repository   the repository identifier
     * @param specPath     the relative spec path
     * @param targetSpecId the target spec version UUID
     * @param baseVersion  the explicitly resolved base version
     * @return the completed breaking change report
     * @throws DiffAnalysisException if analysis fails
     */
    BreakingChangeReport analyzeWithBase(
            String repository, String specPath, java.util.UUID targetSpecId, BaseVersion baseVersion)
            throws DiffAnalysisException;

    /**
     * Registers a change detector for use in the analysis pipeline.
     *
     * @param detector the detector to register
     */
    void registerDetector(ChangeDetector detector);
}
