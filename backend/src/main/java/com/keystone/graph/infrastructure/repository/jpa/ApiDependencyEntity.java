package com.keystone.graph.infrastructure.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code api_dependencies} table in the {@code graph} schema.
 */
@Entity
@Table(
        name = "api_dependencies",
        schema = "graph",
        uniqueConstraints =
                @jakarta.persistence.UniqueConstraint(columnNames = {"producer_id", "consumer_id", "spec_path"}))
public class ApiDependencyEntity {

    @Id
    private UUID id;

    @Column(name = "producer_id", nullable = false)
    private UUID producerId;

    @Column(name = "consumer_id")
    private UUID consumerId;

    @Column(name = "spec_path", nullable = false, length = 512)
    private String specPath;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    protected ApiDependencyEntity() {}

    public ApiDependencyEntity(UUID id, UUID producerId, UUID consumerId, String specPath, Instant discoveredAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.producerId = Objects.requireNonNull(producerId, "producerId must not be null");
        this.consumerId = consumerId;
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.discoveredAt = Objects.requireNonNull(discoveredAt, "discoveredAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getProducerId() {
        return producerId;
    }

    public UUID getConsumerId() {
        return consumerId;
    }

    public String getSpecPath() {
        return specPath;
    }

    public Instant getDiscoveredAt() {
        return discoveredAt;
    }
}
