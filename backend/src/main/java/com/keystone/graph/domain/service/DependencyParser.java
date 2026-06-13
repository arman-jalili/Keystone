// Canonical Reference: .pi/architecture/modules/dependency-graph.md
// Module: dependency-graph
package com.keystone.graph.domain.service;

import com.keystone.graph.domain.exception.UnknownServiceException;
import com.keystone.graph.domain.model.ServiceDeclaration;

/**
 * Domain service interface for parsing {@code keystone.yml} declarations.
 *
 * <p>Per ADR-006, service consumer/producer relationships are declared explicitly
 * via {@code keystone.yml} files placed in each service's repository. This parser
 * processes those declarations and registers the corresponding
 * {@link com.keystone.graph.domain.model.Service} nodes and
 * {@link com.keystone.graph.domain.model.ApiDependency} edges.
 *
 * <p>The implementation must:
 * <ul>
 *   <li>Parse YAML in the format defined by ADR-006</li>
 *   <li>Idempotently handle duplicate registrations (name + spec path)</li>
 *   <li>Skip unknown consumer dependencies with a warning (graceful degradation)</li>
 *   <li>Publish a {@link com.keystone.graph.domain.event.DependencyAddedEvent} on success</li>
 * </ul>
 */
public interface DependencyParser {

    /**
     * Registers a service and its API dependencies from a parsed declaration.
     *
     * <p>If the service already exists, updates its metadata. Creates
     * {@link com.keystone.graph.domain.model.ApiDependency} edges for each
     * produced and consumed spec.
     *
     * @param declaration The parsed service declaration from {@code keystone.yml}
     * @throws UnknownServiceException if a consumed service references an unknown service
     */
    void registerService(ServiceDeclaration declaration) throws UnknownServiceException;

    /**
     * Registers multiple service declarations in batch.
     *
     * <p>Useful when the CLI uploads multiple {@code keystone.yml} files together.
     * Services are registered in declaration order, and unknown service references
     * are skipped with logging rather than failing the entire batch.
     *
     * @param declarations The list of service declarations to register
     */
    void registerServices(Iterable<ServiceDeclaration> declarations);

    /**
     * Removes all registrations for a previously registered service.
     *
     * <p>Deletes the service node and all associated {@code ApiDependency} edges
     * (both as producer and consumer). Used when a service is decommissioned
     * or its {@code keystone.yml} is removed.
     *
     * @param serviceName The name of the service to unregister
     */
    void unregisterService(String serviceName);
}
