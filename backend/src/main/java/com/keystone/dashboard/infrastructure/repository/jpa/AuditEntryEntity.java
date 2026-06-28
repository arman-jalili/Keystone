// Canonical Reference: .pi/architecture/modules/dashboard.md#audit-log-service
package com.keystone.dashboard.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for the {@code audit_log_entries} table.
 */
@Entity
@Table(name = "audit_log_entries")
public class AuditEntryEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 128)
    private String actor;

    @Column(length = 512)
    private String target;

    @Column(length = 2048)
    private String details;

    @Column(nullable = false)
    private Instant timestamp;

    protected AuditEntryEntity() {}

    public AuditEntryEntity(
            String id, String action, String actor, String target, String details, Instant timestamp) {
        this.id = Objects.requireNonNull(id);
        this.action = Objects.requireNonNull(action);
        this.actor = Objects.requireNonNull(actor);
        this.target = target;
        this.details = details;
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public String getId() { return id; }
    public String getAction() { return action; }
    public String getActor() { return actor; }
    public String getTarget() { return target; }
    public String getDetails() { return details; }
    public Instant getTimestamp() { return timestamp; }
}
