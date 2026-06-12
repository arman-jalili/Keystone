package com.keystone.policy.domain.event;

import com.keystone.policy.domain.model.PolicyEvaluationResult;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published after policies have been evaluated against
 * an OpenAPI specification.
 *
 * <p>Consumers subscribe to this event to trigger downstream processing
 * (e.g. breaking change analysis, notifications, audit logging).
 *
 * @param eventId       Unique identifier for this event occurrence
 * @param specId        The UUID of the evaluated OpenAPI specification
 * @param policySetId   The UUID of the evaluated policy set
 * @param repository    The repository identifier (e.g. "org/repo")
 * @param specPath      The relative path of the spec within the repository
 * @param commitSha     The git commit SHA the spec was evaluated from
 * @param verdict       The overall evaluation verdict
 * @param violationCount Number of violations detected
 * @param timestamp     ISO-8601 timestamp of when the evaluation occurred
 */
public record PolicyEvaluatedEvent(
    UUID eventId,
    UUID specId,
    UUID policySetId,
    String repository,
    String specPath,
    String commitSha,
    PolicyEvaluationResult.Verdict verdict,
    int violationCount,
    Instant timestamp
) implements PolicyDomainEvent<PolicyEvaluatedEvent.Payload> {

    public PolicyEvaluatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(specId, "specId must not be null");
        Objects.requireNonNull(policySetId, "policySetId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "PolicyEvaluated";
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
        return new Payload(specId, policySetId, repository, specPath, commitSha, verdict, violationCount);
    }

    /**
     * The data payload carried by a PolicyEvaluated event.
     */
    public record Payload(
        UUID specId,
        UUID policySetId,
        String repository,
        String specPath,
        String commitSha,
        PolicyEvaluationResult.Verdict verdict,
        int violationCount
    ) {}
}
