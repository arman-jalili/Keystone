// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code openapi_specs} table.
 */
@Entity
@Table(name = "openapi_specs")
public class OpenApiSpecEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 256)
    private String repository;

    @Column(name = "spec_path", nullable = false, length = 512)
    private String specPath;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected OpenApiSpecEntity() {}

    public OpenApiSpecEntity(UUID id, String repository, String specPath, Instant ingestedAt) {
        this.id = Objects.requireNonNull(id);
        this.repository = Objects.requireNonNull(repository);
        this.specPath = Objects.requireNonNull(specPath);
        this.ingestedAt = Objects.requireNonNull(ingestedAt);
    }

    public UUID getId() {
        return id;
    }

    public String getRepository() {
        return repository;
    }

    public String getSpecPath() {
        return specPath;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
