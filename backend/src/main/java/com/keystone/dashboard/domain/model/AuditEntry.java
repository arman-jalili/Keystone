// Canonical Reference: .pi/architecture/modules/dashboard.md#audit-log-service
// Implements: Domain model for audit event entries
package com.keystone.dashboard.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * An individual audit log entry from the append-only event store.
 *
 * @param id         Unique event identifier
 * @param action     The action performed (e.g. "SPEC_INGESTED", "POLICY_EVALUATED")
 * @param actor      The user or system actor that performed the action
 * @param target     The target entity (e.g. spec UUID, policy name)
 * @param details    Optional human-readable description
 * @param timestamp  When the action occurred
 */
public record AuditEntry(String id, String action, String actor, String target, String details, Instant timestamp) {

    public AuditEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
