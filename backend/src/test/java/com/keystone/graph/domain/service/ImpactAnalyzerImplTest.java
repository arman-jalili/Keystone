// Canonical Reference: .pi/architecture/modules/dependency-graph.md
package com.keystone.graph.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.keystone.graph.domain.model.ApiDependency;
import com.keystone.graph.domain.model.ImpactAnalysisResult;
import com.keystone.graph.domain.model.ImpactAnalysisResult.AffectedService;
import com.keystone.graph.domain.model.ImpactAnalysisResult.ImpactSeverity;
import com.keystone.graph.domain.model.Service;
import com.keystone.graph.infrastructure.event.GraphEventPublisher;
import com.keystone.graph.infrastructure.repository.GraphRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImpactAnalyzerImplTest {

    @Mock
    private GraphRepository graphRepository;

    @Mock
    private GraphEventPublisher eventPublisher;

    private ImpactAnalyzerImpl impactAnalyzer;

    private final UUID reportId = UUID.randomUUID();
    private final String specPath = "openapi/checkout.yaml";

    // Test services
    private final UUID paymentSvcId = UUID.randomUUID();
    private final UUID userSvcId = UUID.randomUUID();
    private final UUID authSvcId = UUID.randomUUID();
    private final UUID inventorySvcId = UUID.randomUUID();

    private final Service paymentSvc = new Service(paymentSvcId, "payment-svc", "payments");
    private final Service userSvc = new Service(userSvcId, "user-svc", "users");
    private final Service authSvc = new Service(authSvcId, "auth-svc", "auth");
    private final Service inventorySvc = new Service(inventorySvcId, "inventory-svc", "inventory");

    @BeforeEach
    void setUp() {
        impactAnalyzer = new ImpactAnalyzerImpl(graphRepository, eventPublisher);
    }

    @Test
    void computeImpact_withNoProducers_returnsEmptyResult() {
        when(graphRepository.findProducersBySpecPath(specPath)).thenReturn(List.of());

        ImpactAnalysisResult result = impactAnalyzer.computeImpact(specPath, reportId);

        assertThat(result.hasAffectedServices()).isFalse();
        assertThat(result.getReportId()).isEqualTo(reportId);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void computeImpact_withSingleConsumer_returnsDirectImpact() {
        // payment-svc produces checkout.yaml → user-svc consumes it
        when(graphRepository.findProducersBySpecPath(specPath)).thenReturn(List.of(paymentSvc));
        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), paymentSvcId, userSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(userSvcId)).thenReturn(Optional.of(userSvc));
        when(graphRepository.findConsumers(userSvcId)).thenReturn(List.of());

        ImpactAnalysisResult result = impactAnalyzer.computeImpact(specPath, reportId);

        assertThat(result.hasAffectedServices()).isTrue();
        assertThat(result.getAffectedServices()).hasSize(1);

        AffectedService affected = result.getAffectedServices().get(0);
        assertThat(affected.serviceId()).isEqualTo(userSvcId);
        assertThat(affected.serviceName()).isEqualTo("user-svc");
        assertThat(affected.severity()).isEqualTo(ImpactSeverity.DIRECT);
    }

    @Test
    void computeImpact_withCascadingConsumers_returnsDirectAndTransitive() {
        // payment-svc → user-svc → auth-svc (transitive)
        when(graphRepository.findProducersBySpecPath(specPath)).thenReturn(List.of(paymentSvc));
        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), paymentSvcId, userSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(userSvcId)).thenReturn(Optional.of(userSvc));
        when(graphRepository.findConsumers(userSvcId))
                .thenReturn(
                        List.of(new ApiDependency(UUID.randomUUID(), userSvcId, authSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(authSvcId)).thenReturn(Optional.of(authSvc));
        when(graphRepository.findConsumers(authSvcId)).thenReturn(List.of());

        ImpactAnalysisResult result = impactAnalyzer.computeImpact(specPath, reportId);

        assertThat(result.getAffectedServices()).hasSize(2);

        // First affected is DIRECT (user-svc)
        AffectedService direct = result.getAffectedServices().get(0);
        assertThat(direct.serviceName()).isEqualTo("user-svc");
        assertThat(direct.severity()).isEqualTo(ImpactSeverity.DIRECT);

        // Second affected is TRANSITIVE (auth-svc)
        AffectedService transitive = result.getAffectedServices().get(1);
        assertThat(transitive.serviceName()).isEqualTo("auth-svc");
        assertThat(transitive.severity()).isEqualTo(ImpactSeverity.TRANSITIVE);
    }

    @Test
    void computeImpact_withCircularDependency_doesNotInfiniteLoop() {
        // payment-svc → user-svc → auth-svc → payment-svc (circular)
        when(graphRepository.findProducersBySpecPath(specPath)).thenReturn(List.of(paymentSvc));
        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), paymentSvcId, userSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(userSvcId)).thenReturn(Optional.of(userSvc));
        when(graphRepository.findConsumers(userSvcId))
                .thenReturn(
                        List.of(new ApiDependency(UUID.randomUUID(), userSvcId, authSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(authSvcId)).thenReturn(Optional.of(authSvc));
        when(graphRepository.findConsumers(authSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), authSvcId, paymentSvcId, specPath, Instant.now())));

        ImpactAnalysisResult result = impactAnalyzer.computeImpact(specPath, reportId);

        // Should not infinite loop; should find user-svc (DIRECT) and auth-svc (TRANSITIVE)
        assertThat(result.getAffectedServices()).hasSize(2);
    }

    @Test
    void computeImpact_withMultipleProducers_findsAllConsumers() {
        // Two producers: payment-svc and inventory-svc
        // payment-svc → user-svc
        // inventory-svc → auth-svc
        when(graphRepository.findProducersBySpecPath(specPath)).thenReturn(List.of(paymentSvc, inventorySvc));

        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), paymentSvcId, userSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(userSvcId)).thenReturn(Optional.of(userSvc));
        when(graphRepository.findConsumers(userSvcId)).thenReturn(List.of());

        when(graphRepository.findConsumers(inventorySvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), inventorySvcId, authSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(authSvcId)).thenReturn(Optional.of(authSvc));
        when(graphRepository.findConsumers(authSvcId)).thenReturn(List.of());

        ImpactAnalysisResult result = impactAnalyzer.computeImpact(specPath, reportId);

        assertThat(result.getAffectedServices()).hasSize(2);
    }

    @Test
    void computeImpactFromProducer_withValidProducer_returnsImpact() {
        when(graphRepository.findServiceById(paymentSvcId)).thenReturn(Optional.of(paymentSvc));
        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), paymentSvcId, userSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(userSvcId)).thenReturn(Optional.of(userSvc));
        when(graphRepository.findConsumers(userSvcId)).thenReturn(List.of());

        ImpactAnalysisResult result = impactAnalyzer.computeImpactFromProducer(specPath, paymentSvcId, reportId);

        assertThat(result.hasAffectedServices()).isTrue();
        assertThat(result.getAffectedServices()).hasSize(1);
        assertThat(result.getAffectedServices().get(0).serviceName()).isEqualTo("user-svc");
    }

    @Test
    void computeImpactFromProducer_withUnknownProducer_returnsEmpty() {
        when(graphRepository.findServiceById(paymentSvcId)).thenReturn(Optional.empty());

        ImpactAnalysisResult result = impactAnalyzer.computeImpactFromProducer(specPath, paymentSvcId, reportId);

        assertThat(result.hasAffectedServices()).isFalse();
    }

    @Test
    void hasCycle_withNoCycle_returnsFalse() {
        // payment-svc → user-svc (no cycle)
        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), paymentSvcId, userSvcId, specPath, Instant.now())));
        when(graphRepository.findConsumers(userSvcId)).thenReturn(List.of());

        assertThat(impactAnalyzer.hasCycle(paymentSvc)).isFalse();
    }

    @Test
    void hasCycle_withCycle_returnsTrue() {
        // payment-svc → user-svc → payment-svc (cycle)
        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), paymentSvcId, userSvcId, specPath, Instant.now())));
        when(graphRepository.findConsumers(userSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), userSvcId, paymentSvcId, specPath, Instant.now())));

        assertThat(impactAnalyzer.hasCycle(paymentSvc)).isTrue();
    }

    @Test
    void computeImpact_withConsumerThatHasNullConsumerId_skipsIt() {
        // payment-svc produces the spec, has a producer-only edge
        when(graphRepository.findProducersBySpecPath(specPath)).thenReturn(List.of(paymentSvc));
        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(new ApiDependency(UUID.randomUUID(), paymentSvcId, null, specPath, Instant.now())));

        ImpactAnalysisResult result = impactAnalyzer.computeImpact(specPath, reportId);

        assertThat(result.hasAffectedServices()).isFalse();
    }

    @Test
    void computeImpact_dependencyPathIsCorrectlyFormed() {
        // payment-svc → user-svc → auth-svc
        when(graphRepository.findProducersBySpecPath(specPath)).thenReturn(List.of(paymentSvc));
        when(graphRepository.findConsumers(paymentSvcId))
                .thenReturn(List.of(
                        new ApiDependency(UUID.randomUUID(), paymentSvcId, userSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(userSvcId)).thenReturn(Optional.of(userSvc));
        when(graphRepository.findConsumers(userSvcId))
                .thenReturn(
                        List.of(new ApiDependency(UUID.randomUUID(), userSvcId, authSvcId, specPath, Instant.now())));
        when(graphRepository.findServiceById(authSvcId)).thenReturn(Optional.of(authSvc));
        when(graphRepository.findConsumers(authSvcId)).thenReturn(List.of());

        ImpactAnalysisResult result = impactAnalyzer.computeImpact(specPath, reportId);

        AffectedService direct = result.getAffectedServices().get(0);
        assertThat(direct.path()).contains("payment-svc → user-svc");

        AffectedService transitive = result.getAffectedServices().get(1);
        assertThat(transitive.path()).contains("user-svc → auth-svc");
    }
}
