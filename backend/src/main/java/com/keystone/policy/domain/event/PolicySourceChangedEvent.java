// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a policy source configuration changes
 * (e.g. Git repository URL updated, branch changed, credentials rotated).
 *
 * <p>Consumers use this event to refresh connections and invalidate
 * cached policy data from the affected source.
 *
 * @param eventId     Unique identifier for this event occurrence
 * @param sourceId    Identifier of the policy source that changed
 * @param sourceType  The type of source (e.g. "git", "local", "http")
 * @param changeType  The type of change (e.g. "CREATED", "UPDATED", "DELETED")
 * @param timestamp   ISO-8601 timestamp of when the change occurred
 */
public record PolicySourceChangedEvent(
        UUID eventId, String sourceId, String sourceType, ChangeType changeType, Instant timestamp)
        implements PolicyDomainEvent<PolicySourceChangedEvent.Payload> {

    public PolicySourceChangedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "PolicySourceChanged";
    }

    @Override
    public String getSource() {
        return "policy-engine";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Payload getPayload() {
        return new Payload(sourceId, sourceType, changeType);
    }

    /**
     * The data payload carried by a PolicySourceChanged event.
     */
    public record Payload(String sourceId, String sourceType, ChangeType changeType) {}

    /** The type of source configuration change. */
    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}
