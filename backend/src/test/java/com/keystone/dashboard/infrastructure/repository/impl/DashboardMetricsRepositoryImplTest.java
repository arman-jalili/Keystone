package com.keystone.dashboard.infrastructure.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.keystone.analysis.infrastructure.repository.ChangeReportRepository;
import com.keystone.ingestion.domain.model.OpenApiSpec;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import com.keystone.policy.domain.model.Policy;
import com.keystone.policy.domain.model.PolicyScope;
import com.keystone.policy.domain.model.PolicySeverity;
import com.keystone.policy.domain.model.PolicyStatus;
import com.keystone.policy.infrastructure.repository.PolicyRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardMetricsRepositoryImplTest {

    @Mock
    private SpecRepository specRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private ChangeReportRepository changeReportRepository;

    private DashboardMetricsRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new DashboardMetricsRepositoryImpl(specRepository, policyRepository, changeReportRepository);
    }

    @Test
    void findDashboardSummary_shouldReturnZeroForEmptyData() {
        when(specRepository.findAllByOrderByIngestedAtDesc()).thenReturn(List.of());
        when(policyRepository.findAllPolicies(null)).thenReturn(List.of());
        when(changeReportRepository.findLatestReports(anyInt())).thenReturn(List.of());

        var summary = repository.findDashboardSummary();

        assertThat(summary.overallScore()).isEqualTo(1.0);
        assertThat(summary.totalSpecs()).isEqualTo(0);
        assertThat(summary.recentViolations()).isEqualTo(0);
    }

    @Test
    void findDashboardSummary_shouldComputeFromRealData() {
        var spec = new OpenApiSpec(UUID.randomUUID(), "org/repo", "openapi/api.yaml", Instant.now());
        when(specRepository.findAllByOrderByIngestedAtDesc()).thenReturn(List.of(spec));
        when(policyRepository.findAllPolicies(null)).thenReturn(List.of());
        when(changeReportRepository.findLatestReports(anyInt())).thenReturn(List.of());

        var summary = repository.findDashboardSummary();

        assertThat(summary.totalSpecs()).isEqualTo(1);
        assertThat(summary.repositories()).hasSize(1);
        assertThat(summary.repositories().get(0).repositoryId()).isEqualTo("org/repo");
    }

    @Test
    void findMetrics_shouldIncludeSpecAndPolicyCounts() {
        when(specRepository.findAllByOrderByIngestedAtDesc())
                .thenReturn(List.of(new OpenApiSpec(UUID.randomUUID(), "org/repo", "api.yaml", Instant.now())));
        when(policyRepository.findAllPolicies(null)).thenReturn(List.of());
        when(changeReportRepository.findLatestReports(anyInt())).thenReturn(List.of());

        var metrics = repository.findMetrics();

        assertThat(metrics).isNotEmpty();
        assertThat(metrics).anyMatch(m -> m.name().equals("total_specs"));
    }

    @Test
    void findPolicyBreakdown_shouldGroupByStatusAndSeverity() {
        var policy = new Policy(
                UUID.randomUUID(),
                "test-policy",
                "desc",
                PolicySeverity.CRITICAL,
                PolicyStatus.ACTIVE,
                PolicyScope.all(),
                "each yield pass",
                "source",
                1,
                Instant.now(),
                Instant.now());
        when(policyRepository.findAllPolicies(null)).thenReturn(List.of(policy));

        var breakdown = repository.findPolicyBreakdown();

        assertThat(breakdown.totalPolicies()).isEqualTo(1);
        assertThat(breakdown.byStatus()).anyMatch(g -> g.count() == 1);
    }
}
