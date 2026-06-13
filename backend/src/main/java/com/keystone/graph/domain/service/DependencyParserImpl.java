package com.keystone.graph.domain.service;

import com.keystone.graph.domain.event.DependencyAddedEvent;
import com.keystone.graph.domain.exception.UnknownServiceException;
import com.keystone.graph.domain.model.ApiDependency;
import com.keystone.graph.domain.model.ServiceDeclaration;
import com.keystone.graph.domain.model.ServiceDeclaration.SpecConsumed;
import com.keystone.graph.domain.model.ServiceDeclaration.SpecProduced;
import com.keystone.graph.infrastructure.event.GraphEventPublisher;
import com.keystone.graph.infrastructure.repository.GraphRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DependencyParser}.
 *
 * <p>Registers Service nodes and ApiDependency edges from
 * parsed {@code keystone.yml} declarations. Handles idempotent duplicate
 * registrations and gracefully degrades on unknown service references.
 */
@Component
public class DependencyParserImpl implements DependencyParser {

    private static final Logger log = LoggerFactory.getLogger(DependencyParserImpl.class);

    private final GraphRepository graphRepository;
    private final GraphEventPublisher eventPublisher;

    public DependencyParserImpl(GraphRepository graphRepository, GraphEventPublisher eventPublisher) {
        this.graphRepository = graphRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void registerService(ServiceDeclaration declaration) throws UnknownServiceException {
        log.info("Registering service '{}' from declaration", declaration.name());

        // Find or create the service
        com.keystone.graph.domain.model.Service service = graphRepository
                .findServiceByName(declaration.name())
                .orElseGet(() -> {
                    com.keystone.graph.domain.model.Service newService = new com.keystone.graph.domain.model.Service(
                            UUID.randomUUID(), declaration.name(), declaration.team());
                    return graphRepository.saveService(newService);
                });

        int producedCount = 0;
        int consumedCount = 0;

        // Register produced specs
        for (SpecProduced produced : declaration.getProduces()) {
            ApiDependency dep =
                    new ApiDependency(UUID.randomUUID(), service.getId(), null, produced.specPath(), Instant.now());
            try {
                graphRepository.saveDependency(dep);
                producedCount++;
            } catch (Exception e) {
                log.warn("Duplicate producer edge for spec '{}': {}", produced.specPath(), e.getMessage());
            }
        }

        // Register consumed specs
        for (SpecConsumed consumed : declaration.getConsumes()) {
            com.keystone.graph.domain.model.Service producer = graphRepository
                    .findServiceByName(consumed.serviceName())
                    .orElseThrow(
                            () -> UnknownServiceException.forDependency(declaration.name(), consumed.serviceName()));

            ApiDependency dep = new ApiDependency(
                    UUID.randomUUID(),
                    producer.getId(),
                    service.getId(),
                    consumed.specPath() != null ? consumed.specPath() : "",
                    Instant.now());
            try {
                graphRepository.saveDependency(dep);
                consumedCount++;
            } catch (Exception e) {
                log.warn("Duplicate consumer edge for service '{}': {}", consumed.serviceName(), e.getMessage());
            }
        }

        // Publish event
        DependencyAddedEvent event = new DependencyAddedEvent(
                UUID.randomUUID(), declaration.name(), producedCount, consumedCount, Instant.now(), declaration.name());
        eventPublisher.dependencyAdded(event);

        log.info("Service '{}' registered: {} produced, {} consumed", declaration.name(), producedCount, consumedCount);
    }

    @Override
    public void registerServices(Iterable<ServiceDeclaration> declarations) {
        for (ServiceDeclaration declaration : declarations) {
            try {
                registerService(declaration);
            } catch (UnknownServiceException e) {
                log.warn("Skipping registration for '{}': {}", declaration.name(), e.getMessage());
            }
        }
    }

    @Override
    public void unregisterService(String serviceName) {
        log.info("Unregistering service '{}'", serviceName);
        Optional<com.keystone.graph.domain.model.Service> service = graphRepository.findServiceByName(serviceName);
        service.ifPresent(s -> graphRepository.deleteService(s.getId()));
    }
}
