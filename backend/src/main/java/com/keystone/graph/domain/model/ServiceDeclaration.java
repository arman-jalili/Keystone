package com.keystone.graph.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing a service declaration from a {@code keystone.yml} file.
 *
 * <p>Per ADR-006, each service repository contains a {@code keystone.yml} file declaring
 * which API specs the service produces and which external service APIs it consumes.
 * This value object is the parsed representation of that declaration.
 *
 * <p>Parsed by {@link com.keystone.graph.domain.service.DependencyParser} and used
 * to register {@link Service} nodes and {@link ApiDependency} edges in the graph.
 *
 * @param name     The logical name of the service (e.g. "payment-svc")
 * @param team     Optional team identifier (e.g. "payments")
 * @param produces The list of API specs this service produces
 * @param consumes The list of API specs this service consumes from other services
 */
public record ServiceDeclaration(String name, String team, List<SpecProduced> produces, List<SpecConsumed> consumes) {
    public ServiceDeclaration {
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
     * @param specPath The relative path to the spec file within the repository
     * @param version  The version of the API spec (e.g. "v2")
     */
    public record SpecProduced(String specPath, String version) {
        public SpecProduced {
            Objects.requireNonNull(specPath, "specPath must not be null");
        }
    }

    /**
     * An API spec consumed from another service.
     *
     * @param serviceName The name of the service that produces this API
     * @param specPath    Optional — the specific spec path; if null, all specs from the service
     */
    public record SpecConsumed(String serviceName, String specPath) {
        public SpecConsumed {
            Objects.requireNonNull(serviceName, "serviceName must not be null");
        }
    }
}
