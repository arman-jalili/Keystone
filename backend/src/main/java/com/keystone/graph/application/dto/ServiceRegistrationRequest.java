// Canonical Reference: .pi/architecture/modules/dependency-graph.md
// Module: dependency-graph
package com.keystone.graph.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Input DTO for registering a service's API dependencies via the REST API.
 *
 * <p>Received by the {@link com.keystone.graph.interfaces.http.GraphController}
 * endpoint. Mirrors the structure of a {@code keystone.yml} declaration.
 *
 * @param name     The logical name of the service (e.g. "payment-svc")
 * @param team     Optional team identifier (e.g. "payments")
 * @param produces The list of API specs this service produces
 * @param consumes The list of API specs this service consumes from other services
 */
public record ServiceRegistrationRequest(
        @NotBlank(message = "name is required") @Size(max = 256, message = "name must not exceed 256 characters")
                String name,
        @Size(max = 128, message = "team must not exceed 128 characters") String team,
        List<SpecProduced> produces,
        List<SpecConsumed> consumes) {
    public ServiceRegistrationRequest {
        Objects.requireNonNull(name, "name must not be null");
        produces = produces != null ? List.copyOf(produces) : List.of();
        consumes = consumes != null ? List.copyOf(consumes) : List.of();
    }

    public List<SpecProduced> getProduces() {
        return Collections.unmodifiableList(produces);
    }

    public List<SpecConsumed> getConsumes() {
        return Collections.unmodifiableList(consumes);
    }

    /**
     * An API spec produced by a service.
     *
     * @param specPath The relative path to the spec file
     * @param version  The version of the API spec
     */
    public record SpecProduced(
            @NotBlank(message = "specPath is required")
                    @Size(max = 512, message = "specPath must not exceed 512 characters")
                    String specPath,
            @Size(max = 64, message = "version must not exceed 64 characters") String version) {
        public SpecProduced {
            Objects.requireNonNull(specPath, "specPath must not be null");
        }
    }

    /**
     * An API spec consumed from another service.
     *
     * @param serviceName The name of the producer service
     * @param specPath    Optional — if null, all specs from the service
     */
    public record SpecConsumed(
            @NotBlank(message = "serviceName is required")
                    @Size(max = 256, message = "serviceName must not exceed 256 characters")
                    String serviceName,
            @Size(max = 512, message = "specPath must not exceed 512 characters") String specPath) {
        public SpecConsumed {
            Objects.requireNonNull(serviceName, "serviceName must not be null");
        }
    }
}
