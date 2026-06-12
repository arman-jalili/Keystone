package com.keystone.policy.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when policies have been synchronized from
 * an external source (e.g. Git repository).
 *
 * <p>Consumers use this event to invalidate cached policy evaluations
 * and trigger re-evaluation of pending specifications.
 *
 * @param eventId       Unique identifier for this event occurrence
 * @param sourceId      Identifier of the policy source that was synced
 * @param policySetId   The UUID of the synced policy set
 * @param policySetName The name of the synced policy set
 * @param version       The new version number after sync
 * @param policiesAdded   Number of policies added during this sync
 * @param policiesRemoved Number of policies removed during this sync
 * @param policiesUpdated Number of policies updated during this sync
 * @param timestamp     ISO-8601 timestamp of when the sync occurred
 */
public record PolicySyncedEvent(
        UUID eventId,
        String sourceId,
        UUID policySetId,
        String policySetName,
        int version,
        int policiesAdded,
        int policiesRemoved,
        int policiesUpdated,
        Instant timestamp)
        implements PolicyDomainEvent<PolicySyncedEvent.Payload> {

    public PolicySyncedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(policySetId, "policySetId must not be null");
        Objects.requireNonNull(policySetName, "policySetName must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "PolicySynced";
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
        return new Payload(
                sourceId, policySetId, policySetName, version, policiesAdded, policiesRemoved, policiesUpdated);
    }

    /**
     * The data payload carried by a PolicySynced event.
     */
    public record Payload(
            String sourceId,
            UUID policySetId,
            String policySetName,
            int version,
            int policiesAdded,
            int policiesRemoved,
            int policiesUpdated) {}
}
