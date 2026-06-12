package com.keystone.policy.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a single policy rule.
 *
 * <p>A policy defines a rule that OpenAPI specifications are evaluated
 * against. Policies are defined using the Policy DSL (see
 * {@code docs/policy-dsl.md}) and are parsed into this model.
 *
 * <p>Policies are immutable once created.
 */
public class Policy {

    private final UUID id;
    private final String name;
    private final String description;
    private final PolicySeverity severity;
    private final PolicyStatus status;
    private final PolicyScope scope;
    private final String dslExpression;
    private final String sourceId;
    private final int version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Policy(UUID id, String name, String description, PolicySeverity severity,
                  PolicyStatus status, PolicyScope scope, String dslExpression,
                  String sourceId, int version, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.scope = Objects.requireNonNull(scope, "scope must not be null");
        this.dslExpression = Objects.requireNonNull(dslExpression, "dslExpression must not be null");
        this.sourceId = sourceId;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public PolicySeverity getSeverity() { return severity; }
    public PolicyStatus getStatus() { return status; }
    public PolicyScope getScope() { return scope; }
    public String getDslExpression() { return dslExpression; }
    public String getSourceId() { return sourceId; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isActive() {
        return status == PolicyStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Policy policy)) return false;
        return Objects.equals(id, policy.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Policy{id=" + id + ", name='" + name + "', severity=" + severity + "}";
    }
}
