package com.keystone.ingestion.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an OpenAPI specification that has been ingested.
 *
 * <p>Each OpenApiSpec is uniquely identified by its id and is associated with
 * a specific repository and path within that repository. Specs are versioned
 * via {@link SpecVersion} to support audit trails and change tracking.
 *
 * <p>This is a domain entity — no JPA or framework annotations here.
 * The infrastructure layer maps this to a persistent representation.
 */
public class OpenApiSpec {

    private final UUID id;
    private final String repository;
    private final String specPath;
    private final Instant ingestedAt;

    public OpenApiSpec(UUID id, String repository, String specPath, Instant ingestedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.ingestedAt = Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpenApiSpec that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OpenApiSpec{id=" + id + ", repository='" + repository + "', specPath='" + specPath + "'}";
    }
}
