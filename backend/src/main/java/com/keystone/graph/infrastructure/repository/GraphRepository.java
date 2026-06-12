package com.keystone.graph.infrastructure.repository;

import com.keystone.graph.domain.model.ApiDependency;
import com.keystone.graph.domain.model.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for accessing {@link Service} nodes and
 * {@link ApiDependency} edges in the dependency graph.
 *
 * <p>This is the data access contract for the Graph bounded context.
 * The implementation may use Spring Data JPA, raw JDBC, or any other
 * persistence mechanism. Callers must not depend on implementation details
 * such as table names or column mappings.
 *
 * <p>This interface intentionally follows repository pattern conventions
 * while remaining framework-agnostic.
 */
public interface GraphRepository {

    /**
     * Finds a Service by its unique identifier.
     *
     * @param serviceId the service UUID
     * @return the service if found, or empty if not
     */
    Optional<Service> findServiceById(UUID serviceId);

    /**
     * Finds a Service by its logical name.
     *
     * @param name the service name (e.g. "payment-svc")
     * @return the service if found, or empty if not
     */
    Optional<Service> findServiceByName(String name);

    /**
     * Saves a new Service to the data store.
     *
     * @param service the service to save
     * @return the saved service with any generated fields populated
     */
    Service saveService(Service service);

    /**
     * Deletes a Service and all its associated dependency edges.
     *
     * @param serviceId the UUID of the service to delete
     */
    void deleteService(UUID serviceId);

    /**
     * Returns all registered services.
     *
     * @return the list of all services
     */
    List<Service> findAllServices();

    /**
     * Finds all services that produce a given spec.
     *
     * <p>Used by {@link com.keystone.graph.domain.service.ImpactAnalyzer} as the
     * starting point for BFS traversal.
     *
     * @param specPath the spec path to search for
     * @return the list of services that produce the given spec
     */
    List<Service> findProducersBySpecPath(String specPath);

    /**
     * Finds all consumer dependencies for a given producer service.
     *
     * <p>Used by {@link com.keystone.graph.domain.service.ImpactAnalyzer} for BFS
     * traversal to find downstream affected services.
     *
     * @param producerId the UUID of the producer service
     * @return the list of dependency edges where the given service is the producer
     */
    List<ApiDependency> findConsumers(UUID producerId);

    /**
     * Finds all dependencies (edges where the given service is the consumer).
     *
     * @param consumerId the UUID of the consumer service
     * @return the list of dependency edges where the given service is the consumer
     */
    List<ApiDependency> findDependencies(UUID consumerId);

    /**
     * Saves a new ApiDependency edge.
     *
     * <p>If a duplicate edge already exists (same producer, consumer, and specPath),
     * the implementation should return the existing edge without creating a duplicate.
     *
     * @param dependency the dependency edge to save
     * @return the saved dependency with any generated fields populated
     */
    ApiDependency saveDependency(ApiDependency dependency);

    /**
     * Deletes all dependency edges associated with a given service.
     *
     * @param serviceId the UUID of the service whose edges should be deleted
     */
    void deleteDependenciesForService(UUID serviceId);

    /**
     * Deletes a specific dependency edge.
     *
     * @param dependencyId the UUID of the dependency to delete
     */
    void deleteDependency(UUID dependencyId);

    /**
     * Saves a collection of dependency edges in bulk.
     *
     * @param dependencies the dependencies to save
     */
    void saveAllDependencies(Collection<ApiDependency> dependencies);

    /**
     * Finds all consumer dependencies for multiple producer services at once.
     *
     * @param producerIds the UUIDs of the producer services
     * @return the list of dependency edges for all specified producers
     */
    List<ApiDependency> findConsumersForProducers(Collection<UUID> producerIds);
}
