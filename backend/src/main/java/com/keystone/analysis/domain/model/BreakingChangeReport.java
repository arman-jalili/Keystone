// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing the complete result of a breaking change analysis.
 *
 * <p>Produced by {@link com.keystone.analysis.domain.service.DiffOrchestrator} and
 * persisted via {@link com.keystone.analysis.infrastructure.repository.ChangeReportRepository}.
 * Published as a domain event for downstream consumers.
 *
 * <p>Reports are immutable once created.
 */
public class BreakingChangeReport {

    private final UUID id;
    private final UUID baseSpecId;
    private final UUID targetSpecId;
    private final String repository;
    private final String specPath;
    private final String baseVersion;
    private final String targetVersion;
    private final Verdict verdict;
    private final List<Change> changes;
    private final Instant completedAt;

    public BreakingChangeReport(
            UUID id,
            UUID baseSpecId,
            UUID targetSpecId,
            String repository,
            String specPath,
            String baseVersion,
            String targetVersion,
            Verdict verdict,
            List<Change> changes,
            Instant completedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.baseSpecId = Objects.requireNonNull(baseSpecId, "baseSpecId must not be null");
        this.targetSpecId = Objects.requireNonNull(targetSpecId, "targetSpecId must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.specPath = Objects.requireNonNull(specPath, "specPath must not be null");
        this.baseVersion = baseVersion;
        this.targetVersion = Objects.requireNonNull(targetVersion, "targetVersion must not be null");
        this.verdict = Objects.requireNonNull(verdict, "verdict must not be null");
        this.changes = List.copyOf(Objects.requireNonNull(changes, "changes must not be null"));
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getBaseSpecId() {
        return baseSpecId;
    }

    public UUID getTargetSpecId() {
        return targetSpecId;
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

    public String getTargetVersion() {
        return targetVersion;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public List<Change> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean hasBreakingChanges() {
        return changes.stream().anyMatch(c -> c.severity() == ChangeSeverity.BREAKING);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BreakingChangeReport that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BreakingChangeReport{id=" + id + ", repository='" + repository + "', verdict=" + verdict + "}";
    }
}
