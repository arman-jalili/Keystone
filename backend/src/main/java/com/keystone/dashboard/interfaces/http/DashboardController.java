// Canonical Reference: .pi/architecture/modules/dashboard.md#dashboard-controller
// Implements: Dashboard REST API endpoints for health scores, trends, policies
package com.keystone.dashboard.interfaces.http;

import com.keystone.dashboard.application.dto.DashboardSummaryResponse;
import com.keystone.dashboard.application.dto.DashboardTimeRangeRequest;
import com.keystone.dashboard.application.dto.HealthScoreResponse;
import com.keystone.dashboard.application.dto.HealthTrendResponse;
import com.keystone.dashboard.application.dto.PolicyBreakdownResponse;
import com.keystone.dashboard.application.dto.PolicyFilterRequest;
import com.keystone.dashboard.application.dto.PolicySummaryResponse;
import com.keystone.dashboard.application.service.DashboardQueryService;
import com.keystone.dashboard.application.service.PolicyUiService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Dashboard bounded context.
 *
 * <p>Provides endpoints for the dashboard UI including health score
 * visualization, trend analysis, and policy overview. This is the
 * primary entry point for the dashboard module.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/v1/dashboard/summary} — Aggregate dashboard overview</li>
 *   <li>{@code GET /api/v1/dashboard/health/{entityType}/{entityId}} — Health score detail</li>
 *   <li>{@code GET /api/v1/dashboard/health/{entityType}/{entityId}/trend} — Health trend</li>
 *   <li>{@code GET /api/v1/dashboard/policies} — Policy list (filtered, paginated)</li>
 *   <li>{@code GET /api/v1/dashboard/policies/breakdown} — Policy breakdown</li>
 *   <li>{@code GET /api/v1/dashboard/policies/{policyId}} — Single policy detail</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;
    private final PolicyUiService policyUiService;

    public DashboardController(DashboardQueryService dashboardQueryService, PolicyUiService policyUiService) {
        this.dashboardQueryService = dashboardQueryService;
        this.policyUiService = policyUiService;
    }

    /**
     * GET /api/v1/dashboard/summary
     *
     * <p>Retrieves the aggregate dashboard overview including overall health,
     * per-repository summaries, and key metrics.
     *
     * @return 200 OK with the dashboard summary
     */
    @GetMapping(path = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        DashboardSummaryResponse response = dashboardQueryService.getDashboardSummary();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/dashboard/health/{entityType}/{entityId}
     *
     * <p>Retrieves the latest health score for a specific entity.
     *
     * @param entityType the type of entity (e.g. "repository", "spec", "policy-set")
     * @param entityId   the entity identifier
     * @return 200 OK with the health score, 404 if not found
     */
    @GetMapping(path = "/health/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthScoreResponse> getHealthScore(
            @PathVariable String entityType, @PathVariable String entityId) {
        HealthScoreResponse response = dashboardQueryService.getHealthScore(entityType, entityId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/dashboard/health/{entityType}/{entityId}/trend
     *
     * <p>Retrieves the health score trend for an entity over a time range.
     *
     * @param entityType the type of entity
     * @param entityId   the entity identifier
     * @param range      time range filter parameters
     * @return 200 OK with trend data, 404 if not found
     */
    @GetMapping(path = "/health/{entityType}/{entityId}/trend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthTrendResponse> getHealthTrend(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @Valid @ModelAttribute DashboardTimeRangeRequest range) {
        HealthTrendResponse response = dashboardQueryService.getHealthTrend(entityType, entityId, range);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/dashboard/policies
     *
     * <p>Retrieves a paginated, filterable list of policies for the UI.
     *
     * @param filter filter and pagination parameters
     * @return 200 OK with the policy list
     */
    @GetMapping(path = "/policies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PolicySummaryResponse>> listPolicies(@Valid @ModelAttribute PolicyFilterRequest filter) {
        List<PolicySummaryResponse> policies = policyUiService.listPolicies(filter);
        return ResponseEntity.ok(policies);
    }

    /**
     * GET /api/v1/dashboard/policies/breakdown
     *
     * <p>Retrieves the policy breakdown by status and severity for charts.
     *
     * @return 200 OK with the policy breakdown
     */
    @GetMapping(path = "/policies/breakdown", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicyBreakdownResponse> getPolicyBreakdown() {
        PolicyBreakdownResponse breakdown = policyUiService.getPolicyBreakdown();
        return ResponseEntity.ok(breakdown);
    }

    /**
     * GET /api/v1/dashboard/policies/{policyId}
     *
     * <p>Retrieves a single policy summary by ID.
     *
     * @param policyId the policy UUID
     * @return 200 OK with the policy summary, 404 if not found
     */
    @GetMapping(path = "/policies/{policyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicySummaryResponse> getPolicy(@PathVariable String policyId) {
        PolicySummaryResponse policy = policyUiService.getPolicy(policyId);
        return ResponseEntity.ok(policy);
    }
}
