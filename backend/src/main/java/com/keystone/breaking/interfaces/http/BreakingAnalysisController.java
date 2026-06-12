package com.keystone.breaking.interfaces.http;

import com.keystone.breaking.application.dto.AnalysisRequest;
import com.keystone.breaking.application.dto.AnalysisResponse;
import com.keystone.breaking.application.service.BreakingAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the Breaking Change Analysis bounded context.
 *
 * <p>Handles analysis requests triggered by the Keystone CLI and webhook events.
 * This is the primary entry point into the module.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /api/v1/breaking/analyze} — Trigger a new breaking change analysis</li>
 *   <li>{@code POST /api/v1/breaking/analyze/{analysisId}/reanalyze} — Re-run an analysis</li>
 *   <li>{@code GET /api/v1/breaking/analyze/{analysisId} — Get analysis results</li>
 *   <li>{@code GET /api/v1/breaking/repositories/{repository}/latest} — Get latest analysis for a repo</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/breaking")
public class BreakingAnalysisController {

    private final BreakingAnalysisService breakingAnalysisService;

    public BreakingAnalysisController(BreakingAnalysisService breakingAnalysisService) {
        this.breakingAnalysisService = breakingAnalysisService;
    }

    /**
     * POST /api/v1/breaking/analyze
     *
     * <p>Triggers a breaking change analysis for the given specification.
     * The base version is resolved automatically unless explicitly provided.
     *
     * @param request the analysis request payload
     * @return 200 OK with the analysis results
     */
    @PostMapping(path = "/analyze",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> analyze(
            @Valid @RequestBody AnalysisRequest request) {
        AnalysisResponse response = breakingAnalysisService.analyze(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/breaking/analyze/{analysisId}/reanalyze
     *
     * <p>Re-runs a previously completed analysis. Useful when detectors
     * have been updated or when the user wants to re-check with a
     * different configuration.
     *
     * @param analysisId the UUID of the analysis to re-run
     * @return 200 OK with the new analysis results
     */
    @PostMapping(path = "/analyze/{analysisId}/reanalyze",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> reAnalyze(
            @PathVariable("analysisId") UUID analysisId) {
        AnalysisResponse response = breakingAnalysisService.reAnalyze(analysisId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/breaking/analyze/{analysisId}
     *
     * <p>Retrieves the results of a previously completed analysis.
     *
     * @param analysisId the UUID of the analysis to retrieve
     * @return 200 OK with the analysis results
     */
    @GetMapping(path = "/analyze/{analysisId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> getAnalysis(
            @PathVariable("analysisId") UUID analysisId) {
        // TODO: Implement retrieval from DiffReportRepository
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/v1/breaking/repositories/{repository}/latest
     *
     * <p>Retrieves the most recent analysis for a given repository and spec path.
     *
     * @param repository the repository identifier (e.g. "org/repo")
     * @param specPath   the relative spec path within the repository
     * @return 200 OK with the latest analysis, or 404 if none exists
     */
    @GetMapping(path = "/repositories/{repository}/latest",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> getLatestAnalysis(
            @PathVariable("repository") String repository,
            @RequestParam("specPath") String specPath) {
        // TODO: Implement retrieval of latest analysis
        return ResponseEntity.notFound().build();
    }
}
