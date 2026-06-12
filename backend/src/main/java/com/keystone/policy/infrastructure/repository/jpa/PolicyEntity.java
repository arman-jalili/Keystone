package com.keystone.policy.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code policies} table.
 *
 * <p>The policies table is a read-through cache synced from Git by PolicySyncService.
 * All policy mutations go through Git — this table is never written to directly.
 */
@Entity
@Table(name = "policies")
public class PolicyEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "dsl_expression", nullable = false, columnDefinition = "TEXT")
    private String dslExpression;

    @Column(name = "source_id", length = 256)
    private String sourceId;

    @Column(nullable = false)
    private int version;

    @Column(name = "scope_path_patterns", columnDefinition = "TEXT")
    private String scopePathPatterns;

    @Column(name = "scope_operations", length = 256)
    private String scopeOperations;

    @Column(name = "scope_tags", length = 512)
    private String scopeTags;

    @Column(name = "scope_exclude_paths", columnDefinition = "TEXT")
    private String scopeExcludePaths;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PolicyEntity() {}

    public PolicyEntity(UUID id, String name, String description, String severity,
                        String status, String dslExpression, String sourceId, int version,
                        String scopePathPatterns, String scopeOperations,
                        String scopeTags, String scopeExcludePaths,
                        Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.severity = Objects.requireNonNull(severity);
        this.status = Objects.requireNonNull(status);
        this.dslExpression = Objects.requireNonNull(dslExpression);
        this.sourceId = sourceId;
        this.version = version;
        this.scopePathPatterns = scopePathPatterns;
        this.scopeOperations = scopeOperations;
        this.scopeTags = scopeTags;
        this.scopeExcludePaths = scopeExcludePaths;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public String getDslExpression() { return dslExpression; }
    public String getSourceId() { return sourceId; }
    public int getVersion() { return version; }
    public String getScopePathPatterns() { return scopePathPatterns; }
    public String getScopeOperations() { return scopeOperations; }
    public String getScopeTags() { return scopeTags; }
    public String getScopeExcludePaths() { return scopeExcludePaths; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setVersion(int version) { this.version = version; }
    public void setDslExpression(String dslExpression) { this.dslExpression = dslExpression; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
