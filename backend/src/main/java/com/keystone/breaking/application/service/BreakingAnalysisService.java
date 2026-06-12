package com.keystone.breaking.application.service;

import com.keystone.breaking.application.dto.AnalysisRequest;
import com.keystone.breaking.application.dto.AnalysisResponse;
import com.keystone.breaking.domain.exception.DiffAnalysisException;
import com.keystone.breaking.domain.exception.VersionResolutionException;
import com.keystone.breaking.domain.service.DiffOrchestrator;

/**
 * Application service interface for the breaking change analysis use case.
 *
 * <p>This is the primary inbound port (driving adapter) for the
 * breaking-change-analysis module. It defines the contract that the
 * {@link com.keystone.breaking.interfaces.http.BreakingAnalysisController}
 * depends on.
 *
 * <p>Orchestrates the analysis flow:
 * <ol>
 *   <li>Base version resolution via {@link com.keystone.breaking.domain.service.BaseVersionResolver}</li>
 *   <li>Diff analysis via {@link DiffOrchestrator}</li>
 *   <li>Result aggregation and response formatting</li>
 * </ol>
 */
public interface BreakingAnalysisService {

    /**
     * Performs a breaking change analysis for the given specification.
     *
     * <p>Analyses the specification at the given commit SHA against a
     * resolved base version. Returns the complete analysis result
     * including all detected changes.
     *
     * @param request the analysis request containing repository, spec path, and commit SHA
     * @return the full analysis response with detected changes
     * @throws DiffAnalysisException        if the diff analysis pipeline fails
     * @throws VersionResolutionException   if the base version cannot be resolved
     */
    AnalysisResponse analyze(AnalysisRequest request)
            throws DiffAnalysisException, VersionResolutionException;

    /**
     * Re-runs analysis for a previously completed analysis.
     *
     * <p>Useful when detectors have been updated or when the user wants
     * to re-check with a different configuration.
     *
     * @param analysisId the UUID of the previously completed analysis
     * @return the new analysis response
     * @throws DiffAnalysisException if the re-analysis fails
     */
    AnalysisResponse reAnalyze(java.util.UUID analysisId) throws DiffAnalysisException;
}
