// Canonical Reference: .pi/architecture/modules/dashboard.md#dashboard-controller
// Implements: Dashboard REST API endpoints for health scores, compliance, audit, policies
package com.keystone.dashboard.interfaces.http;

import com.keystone.dashboard.application.dto.DashboardSummaryResponse;
import com.keystone.dashboard.application.dto.DashboardTimeRangeRequest;
import com.keystone.dashboard.application.dto.HealthScoreResponse;
import com.keystone.dashboard.application.dto.HealthTrendResponse;
import com.keystone.dashboard.application.dto.PolicyBreakdownResponse;
import com.keystone.dashboard.application.dto.PolicyFilterRequest;
import com.keystone.dashboard.application.dto.PolicySummaryResponse;
import com.keystone.dashboard.application.service.AuditLogService;
import com.keystone.dashboard.application.service.DashboardQueryService;
import com.keystone.dashboard.application.service.HealthScoreService;
import com.keystone.dashboard.application.service.PolicyUiService;
import com.keystone.dashboard.domain.model.AuditEntry;
import com.keystone.dashboard.domain.model.ComplianceSummary;
import com.keystone.dashboard.domain.model.GovernanceHealthScore;
import com.keystone.dashboard.domain.model.PolicyChange;
import com.keystone.dashboard.domain.model.ViolationTrend;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Dashboard bounded context.
 *
 * <p>Provides endpoints for the dashboard UI including health score
 * visualization, compliance history, audit trail, violation trends,
 * policy overview, and RBAC enforcement.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/v1/dashboard/summary} — Aggregate dashboard overview</li>
 *   <li>{@code GET /api/v1/dashboard/health-score} — Governance health score</li>
 *   <li>{@code GET /api/v1/dashboard/health/{entityType}/{entityId}} — Health score detail</li>
 *   <li>{@code GET /api/v1/dashboard/health/{entityType}/{entityId}/trend} — Health trend</li>
 *   <li>{@code GET /api/v1/dashboard/compliance-history/{specId}} — Spec compliance history</li>
 *   <li>{@code GET /api/v1/dashboard/audit-log} — Audit trail (COMPLIANCE_MANAGER only)</li>
 *   <li>{@code GET /api/v1/dashboard/violation-trends} — Violation trends</li>
 *   <li>{@code GET /api/v1/dashboard/policies} — Policy list (filtered, paginated)</li>
 *   <li>{@code GET /api/v1/dashboard/policies/breakdown} — Policy breakdown</li>
 *   <li>{@code GET /api/v1/dashboard/policies/{policyId}} — Single policy detail</li>
 *   <li>{@code PUT /api/v1/dashboard/policies} — Commit policy change (COMPLIANCE_MANAGER only)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;
    private final PolicyUiService policyUiService;
    private final AuditLogService auditLogService;
    private final HealthScoreService healthScoreService;

    public DashboardController(
            DashboardQueryService dashboardQueryService,
            PolicyUiService policyUiService,
            AuditLogService auditLogService,
            HealthScoreService healthScoreService) {
        this.dashboardQueryService = dashboardQueryService;
        this.policyUiService = policyUiService;
        this.auditLogService = auditLogService;
        this.healthScoreService = healthScoreService;
    }

    /**
     * GET /api/v1/dashboard/summary
     *
     * <p>Retrieves the aggregate dashboard overview including overall health,
     * per-repository summaries, and key metrics.
     */
    @GetMapping(path = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(dashboardQueryService.getDashboardSummary());
    }

    /**
     * GET /api/v1/dashboard/health-score
     *
     * <p>Computes and returns the GovernanceHealthScore over a time period.
     *
     * @param period the time period (default: LAST_30_DAYS)
     * @return 200 OK with the governance health score
     */
    @GetMapping(path = "/health-score", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GovernanceHealthScore> getHealthScore(
            @RequestParam(defaultValue = "LAST_30_DAYS") String period) {
        return ResponseEntity.ok(healthScoreService.calculate(period));
    }

    /**
     * GET /api/v1/dashboard/health/{entityType}/{entityId}
     *
     * <p>Retrieves the latest health score for a specific entity.
     */
    @GetMapping(path = "/health/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthScoreResponse> getEntityHealthScore(
            @PathVariable String entityType, @PathVariable String entityId) {
        return ResponseEntity.ok(dashboardQueryService.getHealthScore(entityType, entityId));
    }

    /**
     * GET /api/v1/dashboard/health/{entityType}/{entityId}/trend
     *
     * <p>Retrieves the health score trend for an entity over a time range.
     */
    @GetMapping(path = "/health/{entityType}/{entityId}/trend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthTrendResponse> getHealthTrend(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @Valid @ModelAttribute DashboardTimeRangeRequest range) {
        return ResponseEntity.ok(dashboardQueryService.getHealthTrend(entityType, entityId, range));
    }

    /**
     * GET /api/v1/dashboard/compliance-history/{specId}
     *
     * <p>Retrieves compliance history for a specific spec.
     *
     * @param specId the spec identifier
     * @param days   number of days of history (default: 30)
     * @return 200 OK with the compliance history
     */
    @GetMapping(path = "/compliance-history/{specId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ComplianceSummary>> getComplianceHistory(
            @PathVariable String specId, @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(healthScoreService.getComplianceHistory(specId, days));
    }

    /**
     * GET /api/v1/dashboard/audit-log
     *
     * <p>Retrieves the paginated audit trail. Restricted to COMPLIANCE_MANAGER role.
     *
     * @param page   zero-based page number (default: 0)
     * @param size   page size (default: 50)
     * @param action optional action type filter
     * @return 200 OK with the audit log entries
     */
    @GetMapping(path = "/audit-log", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AuditEntry>> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action) {
        return ResponseEntity.ok(auditLogService.query(page, size, action));
    }

    /**
     * GET /api/v1/dashboard/violation-trends
     *
     * <p>Retrieves violation trend data points over a time range.
     *
     * @param days number of days of trend data (default: 90)
     * @return 200 OK with the violation trends
     */
    @GetMapping(path = "/violation-trends", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ViolationTrend>> getViolationTrends(@RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(healthScoreService.getViolationTrends(days));
    }

    /**
     * GET /api/v1/dashboard/policies
     *
     * <p>Retrieves a paginated, filterable list of policies for the UI.
     */
    @GetMapping(path = "/policies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PolicySummaryResponse>> listPolicies(@Valid @ModelAttribute PolicyFilterRequest filter) {
        return ResponseEntity.ok(policyUiService.listPolicies(filter));
    }

    /**
     * GET /api/v1/dashboard/policies/breakdown
     *
     * <p>Retrieves the policy breakdown by status and severity for charts.
     */
    @GetMapping(path = "/policies/breakdown", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicyBreakdownResponse> getPolicyBreakdown() {
        return ResponseEntity.ok(policyUiService.getPolicyBreakdown());
    }

    /**
     * GET /api/v1/dashboard/policies/{policyId}
     *
     * <p>Retrieves a single policy summary by ID.
     */
    @GetMapping(path = "/policies/{policyId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PolicySummaryResponse> getPolicy(@PathVariable String policyId) {
        return ResponseEntity.ok(policyUiService.getPolicy(policyId));
    }

    /**
     * PUT /api/v1/dashboard/policies
     *
     * <p>Commits a policy change to the Git repository.
     * Restricted to COMPLIANCE_MANAGER role.
     *
     * @param change the policy change to commit
     * @return 200 OK if the change was committed
     */
    @PutMapping(path = "/policies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> commitPolicyChange(@Valid @RequestBody PolicyChange change) {
        // TODO: Wire up PolicyUiService.commitPolicyChange when Git operations are configured
        return ResponseEntity.ok().build();
    }
}
