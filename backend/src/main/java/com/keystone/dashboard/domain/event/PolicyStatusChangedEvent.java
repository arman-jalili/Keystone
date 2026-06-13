package com.keystone.dashboard.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a policy's lifecycle status changes.
 *
 * <p>Consumers: Policy UI (refreshes status display), notification engine
 * (alerts on status changes), audit logging.
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param policyId       The UUID of the policy whose status changed
 * @param policyName     Human-readable name of the policy
 * @param previousStatus The status before the change
 * @param newStatus      The status after the change
 * @param timestamp      ISO-8601 timestamp of when the event occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record PolicyStatusChangedEvent(
        UUID eventId,
        UUID policyId,
        String policyName,
        String previousStatus,
        String newStatus,
        Instant timestamp,
        String idempotencyKey)
        implements DomainEvent<PolicyStatusChangedEvent.Payload> {

    public PolicyStatusChangedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(policyId, "policyId must not be null");
        Objects.requireNonNull(policyName, "policyName must not be null");
        Objects.requireNonNull(previousStatus, "previousStatus must not be null");
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "PolicyStatusChanged";
    }

    @Override
    public String getSource() {
        return "dashboard-engine";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public Payload getPayload() {
        return new Payload(policyId, policyName, previousStatus, newStatus);
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * The data payload carried by a PolicyStatusChanged event.
     */
    public record Payload(
            UUID policyId, String policyName, String previousStatus, String newStatus) {}
}
