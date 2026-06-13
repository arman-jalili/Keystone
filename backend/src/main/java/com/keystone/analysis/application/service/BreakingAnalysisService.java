// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.application.service;

import com.keystone.analysis.application.dto.AnalysisRequest;
import com.keystone.analysis.application.dto.AnalysisResponse;
import com.keystone.analysis.domain.exception.DiffAnalysisException;
import com.keystone.analysis.domain.exception.NoBaseVersionException;
import com.keystone.analysis.domain.service.DiffOrchestrator;

/**
 * Application service interface for the breaking change analysis use case.
 *
 * <p>This is the primary inbound port (driving adapter) for the
 * breaking-change-analysis module. The
 * {@link com.keystone.analysis.interfaces.http.BreakingAnalysisController}
 * depends on this interface.
 *
 * <p>Orchestrates the analysis flow:
 * <ol>
 *   <li>Base version resolution via {@link com.keystone.analysis.domain.service.BaseVersionResolver}</li>
 *   <li>Diff analysis via {@link DiffOrchestrator}</li>
 *   <li>Result aggregation and response formatting</li>
 * </ol>
 */
public interface BreakingAnalysisService {

    /**
     * Performs a breaking change analysis for the given specification.
     *
     * @param request the analysis request
     * @return the full analysis response with detected changes
     * @throws DiffAnalysisException     if the diff analysis pipeline fails
     * @throws NoBaseVersionException    if no base version can be resolved
     */
    AnalysisResponse analyze(AnalysisRequest request) throws DiffAnalysisException, NoBaseVersionException;

    /**
     * Re-runs analysis for a previously completed analysis.
     *
     * @param reportId the UUID of the previously completed report
     * @return the new analysis response
     * @throws DiffAnalysisException if re-analysis fails
     */
    AnalysisResponse reAnalyze(java.util.UUID reportId) throws DiffAnalysisException;
}
