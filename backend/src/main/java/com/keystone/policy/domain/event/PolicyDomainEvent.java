// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for all domain events in the Policy bounded context.
 *
 * <p>Per the event-driven architecture, every domain event carries:
 * <ul>
 *   <li>An event ID for idempotent processing</li>
 *   <li>A source identifier for the publishing context</li>
 *   <li>A timestamp in ISO-8601 format</li>
 * </ul>
 *
 * @param <T> The type of the event payload
 */
public interface PolicyDomainEvent<T> {

    /**
     * Unique identifier for this event occurrence.
     */
    UUID getEventId();

    /**
     * The logical event type name (e.g. "PolicyEvaluated", "PolicySynced").
     */
    String getEventType();

    /**
     * Source context that published this event (e.g. "policy-engine").
     */
    String getSource();

    /**
     * ISO-8601 timestamp of when the event occurred.
     */
    Instant getTimestamp();

    /**
     * The payload carrying event-specific data.
     */
    T getPayload();
}
