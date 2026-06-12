package com.keystone.breaking.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing the complete result of a diff analysis.
 *
 * <p>A {@code DiffReport} is produced by the {@link com.keystone.breaking.domain.service.DiffOrchestrator}
 * and contains all detected breaking changes between a base and new specification version.
 * It is persisted via {@link com.keystone.breaking.infrastructure.repository.DiffReportRepository}
 * and published as a domain event for downstream consumers (e.g. dashboard, notification engine).
 *
 * <p>Reports are immutable once created.
 */
public class DiffReport {

    private final UUID id;
    private final UUID analysisId;
    private final String repository;
    private final String specPath;
    private final String baseVersion;
    private final String newVersion;
    private final String baseCommitSha;
    private final String newCommitSha;
    private final List<BreakingChange> changes;
    private final DiffStatus status;
    private final Instant completedAt;

    public DiffReport(UUID id, UUID analysisId, String repository, String specPath,
                      String baseVersion, String newVersion, String baseCommitSha,
                      String newCommitSha, List<BreakingChange> changes,
                      DiffStatus status, Instant completedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.analysisId = Objects.requireNonNull(analysisId, "analysisId must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.baseVersion = Objects.requireNonNull(baseVersion, "baseVersion must not be null");
        this.newVersion = Objects.requireNonNull(newVersion, "newVersion must not be null");
        this.baseCommitSha = baseCommitSha;
        this.newCommitSha = Objects.requireNonNull(newCommitSha, "newCommitSha must not be null");
        this.changes = List.copyOf(Objects.requireNonNull(changes, "changes must not be null"));
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getAnalysisId() {
        return analysisId;
    }

    public String getRepository() {
        return repository;
    }

    public String getSpecPath() {
        return specPath;
    }

    public String getBaseVersion() {
        return baseVersion;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getBaseCommitSha() {
        return baseCommitSha;
    }

    public String getNewCommitSha() {
        return newCommitSha;
    }

    public List<BreakingChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public boolean hasBreakingChanges() {
        return changes.stream().anyMatch(c -> c.severity() == BreakingChange.Severity.BREAKING);
    }

    public DiffStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiffReport that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DiffReport{id=" + id + ", repository='" + repository + "', status=" + status + "}";
    }

    /**
     * Status of the diff analysis for this report.
     */
    public enum DiffStatus {
        /** Analysis completed successfully with at least one breaking change. */
        BREAKING_FOUND,
        /** Analysis completed successfully with no breaking changes. */
        COMPATIBLE,
        /** Analysis failed due to an error (parse error, network error, etc.). */
        FAILED
    }
}
