package com.keystone.graph.domain.service;

import com.keystone.graph.domain.model.ImpactAnalysisResult;
import com.keystone.graph.domain.model.Service;

import java.util.UUID;

/**
 * Domain service interface for computing the blast radius of a breaking change.
 *
 * <p>Given a breaking change to a spec, performs a BFS traversal of the dependency
 * graph to find all downstream services that may be affected. The traversal starts
 * from the services that produce the changed spec and follows consumer edges.
 *
 * <p>This is the core domain logic of the Dependency Graph bounded context.
 * The implementation must:
 * <ul>
 *   <li>Prevent infinite loops in circular dependency scenarios (visited set)</li>
 *   <li>Classify impact severity as DIRECT or TRANSITIVE</li>
 *   <li>Record the dependency path for each affected service</li>
 * </ul>
 *
 * <p>Consumed by the application service layer to trigger on-demand or event-driven
 * impact analysis.
 */
public interface ImpactAnalyzer {

    /**
     * Computes the impact of a breaking change to the specified spec.
     *
     * <p>Performs BFS traversal starting from services that produce the given spec,
     * following consumer edges to find all downstream affected services.
     *
     * @param specPath The spec path of the changed API (e.g. "openapi/checkout.yaml")
     * @param reportId The ID of the breaking change report that triggered this analysis
     * @return The impact analysis result with all affected downstream services
     */
    ImpactAnalysisResult computeImpact(String specPath, UUID reportId);

    /**
     * Computes the impact of a breaking change scoped to a specific producer service.
     *
     * <p>Useful when the breaking change report identifies the specific service
     * that owns the changed spec, enabling a more targeted traversal.
     *
     * @param specPath    The spec path of the changed API
     * @param producerId  The ID of the service that produces the changed spec
     * @param reportId    The ID of the breaking change report that triggered this analysis
     * @return The impact analysis result with all affected downstream services
     */
    ImpactAnalysisResult computeImpactFromProducer(String specPath, UUID producerId, UUID reportId);

    /**
     * Checks whether the dependency graph contains a cycle involving the given service.
     *
     * @param service The service to check for cycles
     * @return true if a cycle is detected, false otherwise
     */
    boolean hasCycle(Service service);
}
