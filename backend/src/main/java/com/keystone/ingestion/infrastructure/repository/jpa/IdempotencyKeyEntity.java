// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code idempotency_keys} table.
 *
 * <p>Per ADR-007, rows have a 7-day TTL and are cleaned up by a background job.
 */
@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"repository", "commit_sha", "spec_path"})})
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(nullable = false, length = 256)
    private String repository;

    @Column(name = "commit_sha", nullable = false, length = 64)
    private String commitSha;

    @Column(name = "spec_path", nullable = false, length = 512)
    private String specPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyKeyEntity() {}

    public IdempotencyKeyEntity(UUID eventId, String repository, String commitSha, String specPath, Instant createdAt) {
        this.eventId = Objects.requireNonNull(eventId);
        this.repository = Objects.requireNonNull(repository);
        this.commitSha = Objects.requireNonNull(commitSha);
        this.specPath = Objects.requireNonNull(specPath);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getRepository() {
        return repository;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public String getSpecPath() {
        return specPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
