// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code spec_versions} table.
 */
@Entity
@Table(name = "spec_versions")
public class SpecVersionEntity {

    @Id
    private UUID id;

    @Column(name = "spec_id", nullable = false)
    private UUID specId;

    @Column(name = "commit_sha", nullable = false, length = 64)
    private String commitSha;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    protected SpecVersionEntity() {}

    public SpecVersionEntity(
            UUID id, UUID specId, String commitSha, String checksum, String rawContent, Instant ingestedAt) {
        this.id = Objects.requireNonNull(id);
        this.specId = Objects.requireNonNull(specId);
        this.commitSha = Objects.requireNonNull(commitSha);
        this.checksum = Objects.requireNonNull(checksum);
        this.rawContent = Objects.requireNonNull(rawContent);
        this.ingestedAt = Objects.requireNonNull(ingestedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSpecId() {
        return specId;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getRawContent() {
        return rawContent;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
