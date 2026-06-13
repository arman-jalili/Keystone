package com.keystone.graph.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keystone.graph.domain.exception.UnknownServiceException;
import com.keystone.graph.domain.model.ApiDependency;
import com.keystone.graph.domain.model.Service;
import com.keystone.graph.domain.model.ServiceDeclaration;
import com.keystone.graph.domain.model.ServiceDeclaration.SpecConsumed;
import com.keystone.graph.domain.model.ServiceDeclaration.SpecProduced;
import com.keystone.graph.infrastructure.event.GraphEventPublisher;
import com.keystone.graph.infrastructure.repository.GraphRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DependencyParserImplTest {

    @Mock
    private GraphRepository graphRepository;

    @Mock
    private GraphEventPublisher eventPublisher;

    private DependencyParserImpl parser;

    @BeforeEach
    void setUp() {
        parser = new DependencyParserImpl(graphRepository, eventPublisher);
    }

    @Test
    void registerService_createsNewServiceAndEdges() {
        ServiceDeclaration decl = new ServiceDeclaration(
                "payment-svc", "payments", List.of(new SpecProduced("openapi/checkout.yaml", "v2")), List.of());

        when(graphRepository.findServiceByName("payment-svc")).thenReturn(Optional.empty());
        when(graphRepository.saveService(any(Service.class))).thenAnswer(invocation -> {
            Service s = invocation.getArgument(0);
            return new Service(s.getId(), s.getName(), s.getTeam());
        });

        parser.registerService(decl);

        verify(graphRepository).saveService(any(Service.class));
        verify(graphRepository).saveDependency(any(ApiDependency.class));
        verify(eventPublisher).dependencyAdded(any());
    }

    @Test
    void registerService_usesExistingServiceWhenFound() {
        UUID existingId = UUID.randomUUID();
        Service existing = new Service(existingId, "payment-svc", "payments");
        ServiceDeclaration decl = new ServiceDeclaration("payment-svc", "payments", List.of(), List.of());

        when(graphRepository.findServiceByName("payment-svc")).thenReturn(Optional.of(existing));

        parser.registerService(decl);

        verify(graphRepository, never()).saveService(any(Service.class));
        verify(eventPublisher).dependencyAdded(any());
    }

    @Test
    void registerService_registersProducedSpecs() {
        ServiceDeclaration decl = new ServiceDeclaration(
                "payment-svc",
                "payments",
                List.of(
                        new SpecProduced("openapi/checkout.yaml", "v2"),
                        new SpecProduced("openapi/reports.yaml", "v1")),
                List.of());

        when(graphRepository.findServiceByName("payment-svc")).thenReturn(Optional.empty());
        when(graphRepository.saveService(any(Service.class))).thenAnswer(invocation -> {
            Service s = invocation.getArgument(0);
            return new Service(s.getId(), s.getName(), s.getTeam());
        });

        parser.registerService(decl);

        ArgumentCaptor<ApiDependency> depCaptor = ArgumentCaptor.forClass(ApiDependency.class);
        verify(graphRepository, times(2)).saveDependency(depCaptor.capture());

        List<ApiDependency> deps = depCaptor.getAllValues();
        assertThat(deps)
                .extracting(ApiDependency::getSpecPath)
                .contains("openapi/checkout.yaml", "openapi/reports.yaml");
        assertThat(deps).allMatch(d -> d.getConsumerId() == null);
    }

    @Test
    void registerService_registersConsumedSpecs() {
        UUID producerId = UUID.randomUUID();
        Service producer = new Service(producerId, "user-svc", "users");

        ServiceDeclaration decl = new ServiceDeclaration(
                "payment-svc", "payments", List.of(), List.of(new SpecConsumed("user-svc", "openapi/users.yaml")));

        when(graphRepository.findServiceByName("payment-svc")).thenReturn(Optional.empty());
        when(graphRepository.saveService(any(Service.class))).thenAnswer(invocation -> {
            Service s = invocation.getArgument(0);
            return new Service(s.getId(), s.getName(), s.getTeam());
        });
        when(graphRepository.findServiceByName("user-svc")).thenReturn(Optional.of(producer));

        parser.registerService(decl);

        ArgumentCaptor<ApiDependency> depCaptor = ArgumentCaptor.forClass(ApiDependency.class);
        verify(graphRepository).saveDependency(depCaptor.capture());

        ApiDependency dep = depCaptor.getValue();
        assertThat(dep.getProducerId()).isEqualTo(producerId);
        assertThat(dep.getConsumerId()).isNotNull();
        assertThat(dep.getSpecPath()).isEqualTo("openapi/users.yaml");
    }

    @Test
    void registerService_throwsOnUnknownConsumerService() {
        ServiceDeclaration decl = new ServiceDeclaration(
                "payment-svc", "payments", List.of(), List.of(new SpecConsumed("unknown-svc", "openapi/unknown.yaml")));

        when(graphRepository.findServiceByName("payment-svc")).thenReturn(Optional.empty());
        when(graphRepository.saveService(any(Service.class))).thenAnswer(invocation -> {
            Service s = invocation.getArgument(0);
            return new Service(s.getId(), s.getName(), s.getTeam());
        });
        when(graphRepository.findServiceByName("unknown-svc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parser.registerService(decl))
                .isInstanceOf(UnknownServiceException.class)
                .hasMessageContaining("unknown-svc");
    }

    @Test
    void registerService_handlesEmptyConsumedSpecPath() {
        UUID producerId = UUID.randomUUID();
        Service producer = new Service(producerId, "producer-svc", "team-p");

        ServiceDeclaration decl = new ServiceDeclaration(
                "consumer-svc", "team-c", List.of(), List.of(new SpecConsumed("producer-svc", null)));

        when(graphRepository.findServiceByName("consumer-svc")).thenReturn(Optional.empty());
        when(graphRepository.saveService(any(Service.class))).thenAnswer(invocation -> {
            Service s = invocation.getArgument(0);
            return new Service(s.getId(), s.getName(), s.getTeam());
        });
        when(graphRepository.findServiceByName("producer-svc")).thenReturn(Optional.of(producer));

        parser.registerService(decl);

        ArgumentCaptor<ApiDependency> depCaptor = ArgumentCaptor.forClass(ApiDependency.class);
        verify(graphRepository).saveDependency(depCaptor.capture());
        assertThat(depCaptor.getValue().getSpecPath()).isEmpty();
    }

    @Test
    void registerService_publishesEventWithCorrectCounts() {
        ServiceDeclaration decl = new ServiceDeclaration(
                "svc", "t", List.of(new SpecProduced("a.yaml", "v1")), List.of(new SpecConsumed("other", "b.yaml")));

        Service other = new Service(UUID.randomUUID(), "other", "t");

        when(graphRepository.findServiceByName("svc")).thenReturn(Optional.empty());
        when(graphRepository.saveService(any(Service.class))).thenAnswer(invocation -> {
            Service s = invocation.getArgument(0);
            return new Service(s.getId(), s.getName(), s.getTeam());
        });
        when(graphRepository.findServiceByName("other")).thenReturn(Optional.of(other));

        parser.registerService(decl);

        ArgumentCaptor<com.keystone.graph.domain.event.DependencyAddedEvent> eventCaptor =
                ArgumentCaptor.forClass(com.keystone.graph.domain.event.DependencyAddedEvent.class);
        verify(eventPublisher).dependencyAdded(eventCaptor.capture());

        var event = eventCaptor.getValue();
        assertThat(event.serviceName()).isEqualTo("svc");
        assertThat(event.producerCount()).isEqualTo(1);
        assertThat(event.consumerCount()).isEqualTo(1);
    }

    @Test
    void registerServices_skipsFailedDeclarations() {
        ServiceDeclaration valid =
                new ServiceDeclaration("valid-svc", "t", List.of(new SpecProduced("a.yaml", "v1")), List.of());
        ServiceDeclaration invalid = new ServiceDeclaration(
                "invalid-svc", "t", List.of(), List.of(new SpecConsumed("unknown-svc", "b.yaml")));

        when(graphRepository.findServiceByName("valid-svc")).thenReturn(Optional.empty());
        when(graphRepository.saveService(any(Service.class))).thenAnswer(invocation -> {
            Service s = invocation.getArgument(0);
            return new Service(s.getId(), s.getName(), s.getTeam());
        });
        when(graphRepository.findServiceByName("invalid-svc")).thenReturn(Optional.empty());
        when(graphRepository.findServiceByName("unknown-svc")).thenReturn(Optional.empty());

        parser.registerServices(List.of(valid, invalid));

        // Valid should succeed, invalid should be skipped with warning
        verify(eventPublisher, times(1)).dependencyAdded(any());
    }

    @Test
    void unregisterService_removesExistingService() {
        UUID svcId = UUID.randomUUID();
        Service service = new Service(svcId, "svc", "t");

        when(graphRepository.findServiceByName("svc")).thenReturn(Optional.of(service));

        parser.unregisterService("svc");

        verify(graphRepository).deleteService(svcId);
    }

    @Test
    void unregisterService_doesNothingForUnknownService() {
        when(graphRepository.findServiceByName("unknown")).thenReturn(Optional.empty());

        parser.unregisterService("unknown");

        verify(graphRepository, never()).deleteService(any());
    }
}
