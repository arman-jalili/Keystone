package com.keystone.analysis.interfaces.http;

import com.keystone.analysis.application.dto.AnalysisRequest;
import com.keystone.analysis.application.dto.AnalysisResponse;
import com.keystone.analysis.application.service.BreakingAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the Breaking Change Analysis bounded context.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /api/v1/breaking/analyze} — Trigger a new analysis</li>
 *   <li>{@code POST /api/v1/breaking/reports/{reportId}/reanalyze} — Re-run an analysis</li>
 *   <li>{@code GET /api/v1/breaking/reports/{reportId}} — Get report results</li>
 *   <li>{@code GET /api/v1/breaking/repositories/{repository}/latest} — Get latest report</li>
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
     * POST /api/v1/breaking/reports/{reportId}/reanalyze
     *
     * <p>Re-runs a previously completed analysis.
     *
     * @param reportId the UUID of the report to re-run
     * @return 200 OK with the new analysis results
     */
    @PostMapping(path = "/reports/{reportId}/reanalyze",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> reAnalyze(
            @PathVariable("reportId") UUID reportId) {
        AnalysisResponse response = breakingAnalysisService.reAnalyze(reportId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/breaking/reports/{reportId}
     *
     * <p>Retrieves the results of a previously completed analysis.
     *
     * @param reportId the UUID of the report
     * @return 200 OK with the report, or 404 if not found
     */
    @GetMapping(path = "/reports/{reportId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> getReport(
            @PathVariable("reportId") UUID reportId) {
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/v1/breaking/repositories/{repository}/latest
     *
     * <p>Retrieves the most recent report for a given repository and spec path.
     *
     * @param repository the repository identifier
     * @param specPath   the relative spec path
     * @return 200 OK with the latest report, or 404 if none exists
     */
    @GetMapping(path = "/repositories/{repository}/latest",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> getLatestReport(
            @PathVariable("repository") String repository,
            @RequestParam("specPath") String specPath) {
        return ResponseEntity.notFound().build();
    }
}
