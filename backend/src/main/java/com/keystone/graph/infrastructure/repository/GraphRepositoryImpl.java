package com.keystone.graph.infrastructure.repository;

import com.keystone.graph.domain.model.ApiDependency;
import com.keystone.graph.domain.model.Service;
import com.keystone.graph.infrastructure.repository.jpa.ApiDependencyEntity;
import com.keystone.graph.infrastructure.repository.jpa.ServiceEntity;
import com.keystone.graph.infrastructure.repository.jpa.SpringDataApiDependencyRepository;
import com.keystone.graph.infrastructure.repository.jpa.SpringDataServiceRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * JPA-based implementation of {@link GraphRepository}.
 *
 * <p>Uses Spring Data JPA for persistence in the {@code graph} schema.
 * Maps between domain model ({@link Service}, {@link ApiDependency})
 * and JPA entities ({@link ServiceEntity}, {@link ApiDependencyEntity}).
 */
@Repository
public class GraphRepositoryImpl implements GraphRepository {

    private static final Logger log = LoggerFactory.getLogger(GraphRepositoryImpl.class);

    private final SpringDataServiceRepository serviceRepo;
    private final SpringDataApiDependencyRepository depRepo;

    public GraphRepositoryImpl(SpringDataServiceRepository serviceRepo, SpringDataApiDependencyRepository depRepo) {
        this.serviceRepo = serviceRepo;
        this.depRepo = depRepo;
    }

    @Override
    public Optional<Service> findServiceById(UUID serviceId) {
        return serviceRepo.findById(serviceId).map(this::toService);
    }

    @Override
    public Optional<Service> findServiceByName(String name) {
        return serviceRepo.findByName(name).map(this::toService);
    }

    @Override
    public Service saveService(Service service) {
        ServiceEntity entity = new ServiceEntity(
                service.getId(), service.getName(), service.getTeam(), service.getCreatedAt(), service.getUpdatedAt());
        ServiceEntity saved = serviceRepo.save(entity);
        return toService(saved);
    }

    @Override
    public void deleteService(UUID serviceId) {
        depRepo.deleteByProducerIdOrConsumerId(serviceId, serviceId);
        serviceRepo.deleteById(serviceId);
    }

    @Override
    public List<Service> findAllServices() {
        return serviceRepo.findAll().stream().map(this::toService).collect(Collectors.toList());
    }

    @Override
    public List<Service> findProducersBySpecPath(String specPath) {
        return serviceRepo.findProducersBySpecPath(specPath).stream()
                .map(this::toService)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiDependency> findConsumers(UUID producerId) {
        return depRepo.findByProducerId(producerId).stream()
                .map(this::toApiDependency)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiDependency> findDependencies(UUID consumerId) {
        return depRepo.findByConsumerId(consumerId).stream()
                .map(this::toApiDependency)
                .collect(Collectors.toList());
    }

    @Override
    public ApiDependency saveDependency(ApiDependency dependency) {
        ApiDependencyEntity entity = new ApiDependencyEntity(
                dependency.getId(),
                dependency.getProducerId(),
                dependency.getConsumerId(),
                dependency.getSpecPath(),
                dependency.getDiscoveredAt());
        ApiDependencyEntity saved = depRepo.save(entity);
        return toApiDependency(saved);
    }

    @Override
    public void deleteDependenciesForService(UUID serviceId) {
        depRepo.deleteByProducerIdOrConsumerId(serviceId, serviceId);
    }

    @Override
    public void deleteDependency(UUID dependencyId) {
        depRepo.deleteById(dependencyId);
    }

    @Override
    public void saveAllDependencies(Collection<ApiDependency> dependencies) {
        List<ApiDependencyEntity> entities = dependencies.stream()
                .map(d -> new ApiDependencyEntity(
                        d.getId(), d.getProducerId(), d.getConsumerId(), d.getSpecPath(), d.getDiscoveredAt()))
                .collect(Collectors.toList());
        depRepo.saveAll(entities);
    }

    @Override
    public List<ApiDependency> findConsumersForProducers(Collection<UUID> producerIds) {
        return depRepo.findByProducerIdIn(new ArrayList<>(producerIds)).stream()
                .map(this::toApiDependency)
                .collect(Collectors.toList());
    }

    private Service toService(ServiceEntity entity) {
        return new Service(
                entity.getId(),
                entity.getName(),
                entity.getTeam(),
                java.util.Map.of(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private ApiDependency toApiDependency(ApiDependencyEntity entity) {
        return new ApiDependency(
                entity.getId(),
                entity.getProducerId(),
                entity.getConsumerId(),
                entity.getSpecPath(),
                entity.getDiscoveredAt());
    }
}
