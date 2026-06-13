// Canonical Reference: .pi/architecture/modules/dependency-graph.md
// Module: dependency-graph
package com.keystone.graph.application.service;

import com.keystone.graph.application.dto.ImpactAnalysisRequest;
import com.keystone.graph.application.dto.ImpactAnalysisResponse;
import com.keystone.graph.application.dto.ServiceRegistrationRequest;
import com.keystone.graph.application.dto.ServiceRegistrationResponse;
import com.keystone.graph.domain.exception.UnknownServiceException;
import java.util.UUID;

/**
 * Application service interface for the Dependency Graph use case.
 *
 * <p>This is the primary inbound port (driving adapter) for the
 * dependency-graph module. The
 * {@link com.keystone.graph.interfaces.http.GraphController}
 * depends on this interface.
 *
 * <p>Orchestrates the graph management flow:
 * <ol>
 *   <li>Service registration via {@link com.keystone.graph.domain.service.DependencyParser}</li>
 *   <li>Impact analysis via {@link com.keystone.graph.domain.service.ImpactAnalyzer}</li>
 *   <li>Event publication via {@link com.keystone.graph.infrastructure.event.GraphEventPublisher}</li>
 * </ol>
 */
public interface GraphService {

    /**
     * Registers a service and its API dependencies.
     *
     * <p>Parses the declaration and registers {@link com.keystone.graph.domain.model.Service}
     * nodes and {@link com.keystone.graph.domain.model.ApiDependency} edges.
     * If the service already exists, updates its metadata and dependency edges.
     *
     * @param request the service registration request
     * @return the registration result with service details
     * @throws UnknownServiceException if a consumed service references an unknown service
     */
    ServiceRegistrationResponse registerService(ServiceRegistrationRequest request) throws UnknownServiceException;

    /**
     * Computes the impact of a breaking change on downstream services.
     *
     * <p>Delegates to {@link com.keystone.graph.domain.service.ImpactAnalyzer} for
     * BFS traversal, then formats the result as an {@link ImpactAnalysisResponse}.
     *
     * @param request the impact analysis request
     * @return the impact analysis result with affected downstream services
     */
    ImpactAnalysisResponse analyzeImpact(ImpactAnalysisRequest request);

    /**
     * Retrieves a service by its unique identifier.
     *
     * @param serviceId the UUID of the service
     * @return the service registration details, or null if not found
     */
    ServiceRegistrationResponse getService(UUID serviceId);

    /**
     * Removes a service and all its dependency edges from the graph.
     *
     * @param serviceName the name of the service to remove
     */
    void removeService(String serviceName);

    /**
     * Lists all registered services.
     *
     * @return a list of all registered service summaries
     */
    java.util.List<ServiceRegistrationResponse> listServices();
}
