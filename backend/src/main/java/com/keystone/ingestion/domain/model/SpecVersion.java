package com.keystone.ingestion.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a version of an ingested OpenAPI specification.
 *
 * <p>Each SpecVersion records a specific ingestion snapshot:
 * the raw content, the commit SHA it came from, and the checksum
 * used for idempotency. Versions are ordered by {@code ingestedAt}.
 */
public class SpecVersion {

    private final UUID id;
    private final UUID specId;
    private final String commitSha;
    private final String checksum;
    private final String rawContent;
    private final Instant ingestedAt;

    public SpecVersion(UUID id, UUID specId, String commitSha, String checksum,
                       String rawContent, Instant ingestedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.specId = Objects.requireNonNull(specId, "specId must not be null");
        this.commitSha = Objects.requireNonNull(commitSha, "commitSha must not be null");
        this.checksum = Objects.requireNonNull(checksum, "checksum must not be null");
        this.rawContent = Objects.requireNonNull(rawContent, "rawContent must not be null");
        this.ingestedAt = Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpecVersion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SpecVersion{id=" + id + ", specId=" + specId + ", commitSha='" + commitSha + "'}";
    }
}
