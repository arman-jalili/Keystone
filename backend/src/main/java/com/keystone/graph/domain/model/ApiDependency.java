package com.keystone.graph.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a directed dependency edge between two services.
 *
 * <p>An {@code ApiDependency} records that a consumer service depends on
 * an API specification produced by a producer service. The combination of
 * ({@code producerId}, {@code consumerId}, {@code specPath}) is unique —
 * duplicate edges are idempotently ignored.
 *
 * <p>This is the core relationship that the
 * {@link com.keystone.graph.domain.service.ImpactAnalyzer} traverses via BFS
 * to compute the blast radius of a breaking change.
 *
 * <p>Per ADR-006, dependencies are explicitly declared in {@code keystone.yml}
 * files and registered via
 * {@link com.keystone.graph.domain.service.DependencyParser}.
 */
public class ApiDependency {

    private final UUID id;
    private final UUID producerId;
    private final UUID consumerId;
    private final String specPath;
    private final Instant discoveredAt;

    /**
     * @param id           Unique identifier for this dependency edge
     * @param producerId   The service that produces the API
     * @param consumerId   The service that consumes the API (null for producer-only edges)
     * @param specPath     The relative path of the spec within the producer's repository
     * @param discoveredAt When this dependency was first registered
     */
    public ApiDependency(UUID id, UUID producerId, UUID consumerId,
                         String specPath, Instant discoveredAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.producerId = Objects.requireNonNull(producerId, "producerId must not be null");
        this.consumerId = consumerId; // null for producer-only registrations
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.discoveredAt = Objects.requireNonNull(discoveredAt, "discoveredAt must not be null");
    }

    public UUID getId() { return id; }

    public UUID getProducerId() { return producerId; }

    /** Returns the consumer service ID, or null if this is a producer-only edge. */
    public UUID getConsumerId() { return consumerId; }

    public String getSpecPath() { return specPath; }

    public Instant getDiscoveredAt() { return discoveredAt; }

    /**
     * Returns true if this edge represents a consumer relationship
     * (i.e., has both a producer and a consumer).
     */
    public boolean isConsumerEdge() {
        return consumerId != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiDependency that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "ApiDependency{id=" + id + ", producer=" + producerId
               + ", consumer=" + consumerId + ", specPath='" + specPath + "'}";
    }
}
