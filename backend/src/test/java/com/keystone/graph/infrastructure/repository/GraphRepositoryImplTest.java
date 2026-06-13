// Canonical Reference: .pi/architecture/modules/dependency-graph.md
package com.keystone.graph.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.graph.domain.model.ApiDependency;
import com.keystone.graph.domain.model.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(GraphRepositoryImpl.class)
@ActiveProfiles("test")
class GraphRepositoryImplTest {

    @Autowired
    private GraphRepositoryImpl graphRepository;

    @Test
    void findServiceById_shouldReturnEmptyForUnknownId() {
        Optional<Service> result = graphRepository.findServiceById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void saveAndFindServiceById_shouldRoundTrip() {
        UUID serviceId = UUID.randomUUID();
        Service service = new Service(serviceId, "payment-svc", "payments");

        Service saved = graphRepository.saveService(service);
        assertThat(saved.getId()).isEqualTo(serviceId);
        assertThat(saved.getName()).isEqualTo("payment-svc");

        Optional<Service> found = graphRepository.findServiceById(serviceId);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("payment-svc");
        assertThat(found.get().getTeam()).isEqualTo("payments");
    }

    @Test
    void findServiceByName_shouldReturnService() {
        Service service = new Service(UUID.randomUUID(), "user-svc", "users");
        graphRepository.saveService(service);

        Optional<Service> found = graphRepository.findServiceByName("user-svc");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("user-svc");
    }

    @Test
    void findServiceByName_shouldReturnEmptyForUnknown() {
        Optional<Service> found = graphRepository.findServiceByName("unknown-svc");
        assertThat(found).isEmpty();
    }

    @Test
    void findAllServices_shouldReturnAllServices() {
        graphRepository.saveService(new Service(UUID.randomUUID(), "svc-a", "team-a"));
        graphRepository.saveService(new Service(UUID.randomUUID(), "svc-b", "team-b"));

        List<Service> all = graphRepository.findAllServices();
        assertThat(all).hasSize(2);
    }

    @Test
    void deleteService_shouldRemoveServiceAndDependencies() {
        UUID producerId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();
        graphRepository.saveService(new Service(producerId, "producer-svc", "team-p"));
        graphRepository.saveService(new Service(consumerId, "consumer-svc", "team-c"));

        ApiDependency dep = new ApiDependency(UUID.randomUUID(), producerId, consumerId, "openapi.yaml", Instant.now());
        graphRepository.saveDependency(dep);

        // Verify dependency exists
        assertThat(graphRepository.findConsumers(producerId)).hasSize(1);

        // Delete the producer
        graphRepository.deleteService(producerId);

        // Verify producer is gone
        assertThat(graphRepository.findServiceById(producerId)).isEmpty();
        // Verify dependencies are also cleaned up
        assertThat(graphRepository.findConsumers(producerId)).isEmpty();
    }

    @Test
    void saveAndFindDependency_shouldRoundTrip() {
        UUID producerId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();
        graphRepository.saveService(new Service(producerId, "producer-svc", "team-p"));
        graphRepository.saveService(new Service(consumerId, "consumer-svc", "team-c"));

        UUID depId = UUID.randomUUID();
        ApiDependency dep = new ApiDependency(depId, producerId, consumerId, "openapi/checkout.yaml", Instant.now());
        ApiDependency saved = graphRepository.saveDependency(dep);

        assertThat(saved.getId()).isEqualTo(depId);
        assertThat(saved.getProducerId()).isEqualTo(producerId);
        assertThat(saved.getConsumerId()).isEqualTo(consumerId);
        assertThat(saved.getSpecPath()).isEqualTo("openapi/checkout.yaml");
    }

    @Test
    void findConsumers_shouldReturnConsumerEdges() {
        UUID producerId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();
        graphRepository.saveService(new Service(producerId, "producer-svc", "team-p"));
        graphRepository.saveService(new Service(consumerId, "consumer-svc", "team-c"));

        graphRepository.saveDependency(
                new ApiDependency(UUID.randomUUID(), producerId, consumerId, "openapi.yaml", Instant.now()));

        List<ApiDependency> consumers = graphRepository.findConsumers(producerId);
        assertThat(consumers).hasSize(1);
        assertThat(consumers.get(0).getConsumerId()).isEqualTo(consumerId);
    }

    @Test
    void findProducersBySpecPath_shouldReturnProducers() {
        UUID svcId = UUID.randomUUID();
        String specPath = "openapi/checkout.yaml";
        graphRepository.saveService(new Service(svcId, "checkout-svc", "team-c"));

        // Register a producer edge
        graphRepository.saveDependency(new ApiDependency(UUID.randomUUID(), svcId, null, specPath, Instant.now()));

        List<Service> producers = graphRepository.findProducersBySpecPath(specPath);
        assertThat(producers).hasSize(1);
        assertThat(producers.get(0).getName()).isEqualTo("checkout-svc");
    }

    @Test
    void findConsumersForProducers_shouldReturnAllEdges() {
        UUID prod1Id = UUID.randomUUID();
        UUID prod2Id = UUID.randomUUID();
        UUID consId = UUID.randomUUID();
        graphRepository.saveService(new Service(prod1Id, "prod-1", "t1"));
        graphRepository.saveService(new Service(prod2Id, "prod-2", "t2"));
        graphRepository.saveService(new Service(consId, "cons", "t3"));

        graphRepository.saveDependency(new ApiDependency(UUID.randomUUID(), prod1Id, consId, "s.yaml", Instant.now()));
        graphRepository.saveDependency(new ApiDependency(UUID.randomUUID(), prod2Id, consId, "s.yaml", Instant.now()));

        List<ApiDependency> edges = graphRepository.findConsumersForProducers(List.of(prod1Id, prod2Id));
        assertThat(edges).hasSize(2);
    }

    @Test
    void saveAllDependencies_shouldSaveInBatch() {
        UUID prodId = UUID.randomUUID();
        UUID cons1Id = UUID.randomUUID();
        UUID cons2Id = UUID.randomUUID();
        graphRepository.saveService(new Service(prodId, "prod", "t"));
        graphRepository.saveService(new Service(cons1Id, "c1", "t"));
        graphRepository.saveService(new Service(cons2Id, "c2", "t"));

        List<ApiDependency> deps = List.of(
                new ApiDependency(UUID.randomUUID(), prodId, cons1Id, "s.yaml", Instant.now()),
                new ApiDependency(UUID.randomUUID(), prodId, cons2Id, "s.yaml", Instant.now()));

        graphRepository.saveAllDependencies(deps);

        assertThat(graphRepository.findConsumers(prodId)).hasSize(2);
    }

    @Test
    void deleteDependency_shouldRemoveSpecificEdge() {
        UUID prodId = UUID.randomUUID();
        UUID consId = UUID.randomUUID();
        graphRepository.saveService(new Service(prodId, "prod", "t"));
        graphRepository.saveService(new Service(consId, "cons", "t"));

        UUID depId = UUID.randomUUID();
        graphRepository.saveDependency(new ApiDependency(depId, prodId, consId, "s.yaml", Instant.now()));

        assertThat(graphRepository.findConsumers(prodId)).hasSize(1);

        graphRepository.deleteDependency(depId);

        assertThat(graphRepository.findConsumers(prodId)).isEmpty();
    }

    @Test
    void deleteDependenciesForService_shouldRemoveAllEdges() {
        UUID prodId = UUID.randomUUID();
        UUID cons1Id = UUID.randomUUID();
        UUID cons2Id = UUID.randomUUID();
        graphRepository.saveService(new Service(prodId, "prod", "t"));
        graphRepository.saveService(new Service(cons1Id, "c1", "t"));
        graphRepository.saveService(new Service(cons2Id, "c2", "t"));

        graphRepository.saveDependency(new ApiDependency(UUID.randomUUID(), prodId, cons1Id, "s.yaml", Instant.now()));
        graphRepository.saveDependency(new ApiDependency(UUID.randomUUID(), prodId, cons2Id, "s.yaml", Instant.now()));

        assertThat(graphRepository.findConsumers(prodId)).hasSize(2);

        graphRepository.deleteDependenciesForService(prodId);

        assertThat(graphRepository.findConsumers(prodId)).isEmpty();
    }

    @Test
    void findDependencies_shouldReturnEdgesWhereServiceIsConsumer() {
        UUID prodId = UUID.randomUUID();
        UUID consId = UUID.randomUUID();
        graphRepository.saveService(new Service(prodId, "prod", "t"));
        graphRepository.saveService(new Service(consId, "cons", "t"));

        graphRepository.saveDependency(new ApiDependency(UUID.randomUUID(), prodId, consId, "s.yaml", Instant.now()));

        List<ApiDependency> deps = graphRepository.findDependencies(consId);
        assertThat(deps).hasSize(1);
        assertThat(deps.get(0).getProducerId()).isEqualTo(prodId);
    }
}
