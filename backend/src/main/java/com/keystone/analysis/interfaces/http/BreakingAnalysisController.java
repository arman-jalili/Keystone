// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.interfaces.http;

import com.keystone.analysis.application.dto.AnalysisRequest;
import com.keystone.analysis.application.dto.AnalysisResponse;
import com.keystone.analysis.application.service.BreakingAnalysisService;
import com.keystone.analysis.domain.exception.DiffAnalysisException;
import com.keystone.analysis.domain.exception.NoBaseVersionException;
import com.keystone.analysis.infrastructure.repository.ChangeReportRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Breaking Change Analysis bounded context.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /api/v1/breaking/analyze} — Trigger a new analysis</li>
 *   <li>{@code POST /api/v1/breaking/reports/{reportId}/reanalyze} — Re-run an analysis</li>
 *   <li>{@code GET /api/v1/breaking/reports/{reportId}} — Get report results</li>
 *   <li>{@code GET /api/v1/breaking/reports/latest} — List recent reports across all repos (Dashboard)</li>
 *   <li>{@code GET /api/v1/breaking/repositories/{repository}/latest} — Get latest report for a repo</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/breaking")
public class BreakingAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(BreakingAnalysisController.class);

    private final BreakingAnalysisService breakingAnalysisService;
    private final ChangeReportRepository reportRepository;

    public BreakingAnalysisController(
            BreakingAnalysisService breakingAnalysisService, ChangeReportRepository reportRepository) {
        this.breakingAnalysisService = breakingAnalysisService;
        this.reportRepository = reportRepository;
    }

    /**
     * POST /api/v1/breaking/analyze
     *
     * <p>Triggers a breaking change analysis for the given specification.
     *
     * @param request the analysis request payload
     * @return 200 OK with the analysis results, or 422 if analysis fails
     */
    @PostMapping(
            path = "/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalysisRequest request) {
        try {
            AnalysisResponse response = breakingAnalysisService.analyze(request);
            return ResponseEntity.ok(response);
        } catch (NoBaseVersionException e) {
            log.warn("Analysis requested with no base version: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (DiffAnalysisException e) {
            log.error("Analysis failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    /**
     * POST /api/v1/breaking/reports/{reportId}/reanalyze
     *
     * <p>Re-runs a previously completed analysis.
     *
     * @param reportId the UUID of the report to re-run
     * @return 200 OK with the new analysis results
     */
    @PostMapping(path = "/reports/{reportId}/reanalyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> reAnalyze(@PathVariable("reportId") UUID reportId) {
        try {
            AnalysisResponse response = breakingAnalysisService.reAnalyze(reportId);
            return ResponseEntity.ok(response);
        } catch (DiffAnalysisException e) {
            log.error("Re-analysis failed for {}: {}", reportId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/v1/breaking/reports/{reportId}
     *
     * <p>Retrieves the results of a previously completed analysis.
     *
     * @param reportId the UUID of the report
     * @return 200 OK with the report, or 404 if not found
     */
    @GetMapping(path = "/reports/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> getReport(@PathVariable("reportId") UUID reportId) {
        return reportRepository
                .findById(reportId)
                .map(report -> ResponseEntity.ok(AnalysisResponse.from(report)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/breaking/reports/latest
     *
     * <p>Retrieves the most recent breaking change reports across all repositories.
     * Used by the Dashboard Breaking Changes view to show a flat list of recent reports.
     *
     * @param limit the maximum number of reports to return (default: 50)
     * @return 200 OK with the list of recent reports
     */
    @GetMapping(path = "/reports/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AnalysisResponse>> getLatestReports(
            @RequestParam(defaultValue = "50") int limit) {
        List<AnalysisResponse> reports = reportRepository.findLatestReports(limit).stream()
                .map(AnalysisResponse::from)
                .toList();
        return ResponseEntity.ok(reports);
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
    @GetMapping(path = "/repositories/{repository}/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> getLatestReport(
            @PathVariable("repository") String repository, @RequestParam("specPath") String specPath) {
        return reportRepository
                .findLatestByRepositoryAndSpecPath(repository, specPath)
                .map(report -> ResponseEntity.ok(AnalysisResponse.from(report)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
