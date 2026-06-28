// Canonical Reference: .pi/architecture/modules/dashboard.md#dashboard-controller
// Implements: Repository implementation for aggregate dashboard metrics
package com.keystone.dashboard.infrastructure.repository.impl;

import com.keystone.analysis.domain.model.Verdict;
import com.keystone.analysis.infrastructure.repository.ChangeReportRepository;
import com.keystone.dashboard.domain.model.DashboardMetric;
import com.keystone.dashboard.domain.model.DashboardSummary;
import com.keystone.dashboard.domain.model.HealthTrend;
import com.keystone.dashboard.domain.model.PolicyBreakdown;
import com.keystone.dashboard.infrastructure.repository.DashboardMetricsRepository;
import com.keystone.dashboard.domain.model.PolicySummary;
import com.keystone.ingestion.domain.model.OpenApiSpec;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Computes aggregate dashboard metrics from across all bounded contexts.
 *
 * <p>Queries Ingestion, Policy, and Analysis repositories to build the
 * dashboard summary, KPI metrics, and policy breakdown.
 */
@Repository
public class DashboardMetricsRepositoryImpl implements DashboardMetricsRepository {

    private final SpecRepository specRepository;
    private final PolicyRepository policyRepository;
    private final ChangeReportRepository changeReportRepository;

    public DashboardMetricsRepositoryImpl(
            SpecRepository specRepository,
            PolicyRepository policyRepository,
            ChangeReportRepository changeReportRepository) {
        this.specRepository = specRepository;
        this.policyRepository = policyRepository;
        this.changeReportRepository = changeReportRepository;
    }

    @Override
    public DashboardSummary findDashboardSummary() {
        var allSpecs = specRepository.findAllByOrderByIngestedAtDesc();
        var allPolicies = policyRepository.findAllPolicies(null);
        var recentReports = changeReportRepository.findLatestReports(100);

        int totalSpecs = allSpecs.size();
        long activePolicies = allPolicies.stream()
                .filter(p -> "ACTIVE".equals(p.getStatus().name()))
                .count();
        long recentViolations = recentReports.stream()
                .filter(r -> r.getVerdict() != Verdict.PASS)
                .count();

        double overallScore = totalSpecs > 0
                ? Math.min(1.0, (double) activePolicies / Math.max(1, totalSpecs))
                : 1.0;

        // Per-repository health summaries grouped by repository
        Map<String, List<OpenApiSpec>> specsByRepo = allSpecs.stream()
                .collect(Collectors.groupingBy(OpenApiSpec::getRepository));

        var repositories = specsByRepo.entrySet().stream()
                .map(entry -> {
                    String repoId = entry.getKey();
                    int specCount = entry.getValue().size();
                    long violationsForRepo = recentReports.stream()
                            .filter(r -> r.getRepository().equals(repoId))
                            .filter(r -> r.getVerdict() != Verdict.PASS)
                            .count();
                    double healthScore = violationsForRepo > 0
                            ? Math.max(0.0, 1.0 - (violationsForRepo / 10.0))
                            : 1.0;
                    return new DashboardSummary.RepositoryHealth(
                            repoId,
                            healthScore,
                            specCount,
                            (int) violationsForRepo,
                            HealthTrend.TrendDirection.STABLE);
                })
                .toList();

        return new DashboardSummary(overallScore, repositories, totalSpecs, (int) activePolicies, (int) recentViolations);
    }

    @Override
    public List<DashboardMetric> findMetrics() {
        var allSpecs = specRepository.findAllByOrderByIngestedAtDesc();
        var allPolicies = policyRepository.findAllPolicies(null);
        var recentReports = changeReportRepository.findLatestReports(100);

        long breakingChanges = recentReports.stream()
                .filter(r -> r.getVerdict() == Verdict.BREAKING)
                .count();
        long passingChanges = recentReports.stream()
                .filter(r -> r.getVerdict() == Verdict.PASS)
                .count();
        long activePolicies = allPolicies.stream()
                .filter(p -> "ACTIVE".equals(p.getStatus().name()))
                .count();

        return List.of(
                new DashboardMetric(
                        "total_specs", "Total Specs", allSpecs.size(), "specs",
                        new DashboardMetric.MetricChange(
                                DashboardMetric.MetricChange.ChangeDirection.FLAT, 0, 0.0)),
                new DashboardMetric(
                        "active_policies", "Active Policies", activePolicies, "policies",
                        new DashboardMetric.MetricChange(
                                DashboardMetric.MetricChange.ChangeDirection.FLAT, 0, 0.0)),
                new DashboardMetric(
                        "breaking_changes", "Breaking Changes", breakingChanges, "changes",
                        new DashboardMetric.MetricChange(
                                breakingChanges > 0
                                        ? DashboardMetric.MetricChange.ChangeDirection.NEGATIVE
                                        : DashboardMetric.MetricChange.ChangeDirection.POSITIVE,
                                breakingChanges, 0.0)),
                new DashboardMetric(
                        "passing_rate", "Passing Rate",
                        recentReports.isEmpty() ? 100.0
                                : (double) passingChanges / recentReports.size() * 100.0,
                        "%",
                        new DashboardMetric.MetricChange(
                                DashboardMetric.MetricChange.ChangeDirection.FLAT, 0, 0.0)));
    }

    @Override
    public PolicyBreakdown findPolicyBreakdown() {
        var allPolicies = policyRepository.findAllPolicies(null);

        long active = allPolicies.stream()
                .filter(p -> "ACTIVE".equals(p.getStatus().name()))
                .count();
        long inactive = allPolicies.stream()
                .filter(p -> "INACTIVE".equals(p.getStatus().name()))
                .count();

        long critical = allPolicies.stream()
                .filter(p -> "CRITICAL".equals(p.getSeverity().name()))
                .count();
        long high = allPolicies.stream()
                .filter(p -> "HIGH".equals(p.getSeverity().name()))
                .count();
        long medium = allPolicies.stream()
                .filter(p -> "MEDIUM".equals(p.getSeverity().name()))
                .count();
        long low = allPolicies.stream()
                .filter(p -> "LOW".equals(p.getSeverity().name()))
                .count();

        double overallCompliance = allPolicies.isEmpty() ? 1.0 : (double) active / allPolicies.size();

        return new PolicyBreakdown(
                allPolicies.size(),
                List.of(
                        new PolicyBreakdown.StatusGroup(PolicySummary.PolicyStatus.ACTIVE, (int) active),
                        new PolicyBreakdown.StatusGroup(PolicySummary.PolicyStatus.INACTIVE, (int) inactive)),
                List.of(
                        new PolicyBreakdown.SeverityGroup(PolicySummary.PolicySeverity.CRITICAL, (int) critical),
                        new PolicyBreakdown.SeverityGroup(PolicySummary.PolicySeverity.HIGH, (int) high),
                        new PolicyBreakdown.SeverityGroup(PolicySummary.PolicySeverity.MEDIUM, (int) medium),
                        new PolicyBreakdown.SeverityGroup(PolicySummary.PolicySeverity.LOW, (int) low)),
                overallCompliance);
    }
}
