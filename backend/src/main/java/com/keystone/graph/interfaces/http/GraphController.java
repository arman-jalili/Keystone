package com.keystone.graph.interfaces.http;

import com.keystone.graph.application.dto.ImpactAnalysisRequest;
import com.keystone.graph.application.dto.ImpactAnalysisResponse;
import com.keystone.graph.application.dto.ServiceRegistrationRequest;
import com.keystone.graph.application.dto.ServiceRegistrationResponse;
import com.keystone.graph.application.service.GraphService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the Dependency Graph bounded context.
 *
 * <p>Exposes endpoints for registering service dependencies, querying the graph,
 * and computing impact analysis for breaking changes.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /api/v1/graph/services} — Register a service and its dependencies</li>
 *   <li>{@code GET /api/v1/graph/services} — List all registered services</li>
 *   <li>{@code GET /api/v1/graph/services/{id}} — Get a specific service</li>
 *   <li>{@code DELETE /api/v1/graph/services/{name}} — Remove a service</li>
 *   <li>{@code POST /api/v1/graph/impact} — Compute impact analysis</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * POST /api/v1/graph/services
     *
     * <p>Registers a service and its API dependencies. If the service already exists,
     * updates its metadata and dependency edges.
     *
     * @param request the service registration payload
     * @return 201 Created for new service, 200 OK for update
     */
    @PostMapping(path = "/services",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceRegistrationResponse> registerService(
            @Valid @RequestBody ServiceRegistrationRequest request) {
        ServiceRegistrationResponse response = graphService.registerService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/graph/services
     *
     * <p>Lists all registered services.
     *
     * @return 200 OK with the list of services
     */
    @GetMapping(path = "/services",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ServiceRegistrationResponse>> listServices() {
        List<ServiceRegistrationResponse> services = graphService.listServices();
        return ResponseEntity.ok(services);
    }

    /**
     * GET /api/v1/graph/services/{id}
     *
     * <p>Retrieves a specific service by its UUID.
     *
     * @param id the service UUID
     * @return 200 OK with the service details, or 404 Not Found
     */
    @GetMapping(path = "/services/{id}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceRegistrationResponse> getService(@PathVariable UUID id) {
        ServiceRegistrationResponse response = graphService.getService(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/graph/services/{name}
     *
     * <p>Removes a service and all its dependency edges from the graph.
     *
     * @param name the service name
     * @return 204 No Content
     */
    @DeleteMapping(path = "/services/{name}",
                   produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> removeService(@PathVariable String name) {
        graphService.removeService(name);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/graph/impact
     *
     * <p>Computes the blast radius of a breaking change. Performs BFS traversal
     * to find all downstream services that may be affected.
     *
     * @param request the impact analysis request
     * @return 200 OK with the impact analysis result
     */
    @PostMapping(path = "/impact",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImpactAnalysisResponse> analyzeImpact(
            @Valid @RequestBody ImpactAnalysisRequest request) {
        ImpactAnalysisResponse response = graphService.analyzeImpact(request);
        return ResponseEntity.ok(response);
    }
}
