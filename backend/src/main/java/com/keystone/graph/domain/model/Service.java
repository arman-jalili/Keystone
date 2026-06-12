package com.keystone.graph.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a registered service in the dependency graph.
 *
 * <p>A {@code Service} is the primary entity in the Graph bounded context.
 * It represents a microservice or application that either produces or consumes
 * API specifications. Services are registered via {@code keystone.yml}
 * declarations parsed by {@link com.keystone.graph.domain.service.DependencyParser}.
 *
 * <p>Each service has a unique name (typically "owner/repo" or logical service name)
 * and optional metadata such as owning team and associated endpoints.
 *
 * <p>Services are persisted via {@link com.keystone.graph.infrastructure.repository.GraphRepository}.
 */
public class Service {

    private final UUID id;
    private final String name;
    private final String team;
    private final Map<String, Object> metadata;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Service(
            UUID id, String name, String team, Map<String, Object> metadata, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.team = team;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /** Convenience constructor for new service creation. */
    public Service(UUID id, String name, String team) {
        this(id, name, team, Map.of(), Instant.now(), Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTeam() {
        return team;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service service)) return false;
        return Objects.equals(id, service.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Service{id=" + id + ", name='" + name + "', team='" + team + "'}";
    }
}
