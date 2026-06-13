// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.infrastructure.event;

import com.keystone.policy.domain.event.PolicyEvaluatedEvent;
import com.keystone.policy.domain.event.PolicySourceChangedEvent;
import com.keystone.policy.domain.event.PolicySyncedEvent;

/**
 * Outbound port for publishing policy-related domain events.
 *
 * <p>Per the event-driven architecture, policy events are published
 * via the event bus. The initial implementation uses Spring's
 * {@code ApplicationEventPublisher} (in-process), with the option
 * to swap for Redis Streams or Kafka later.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Publish events synchronously or asynchronously (configurable)</li>
 *   <li>Ensure at-least-once delivery semantics</li>
 *   <li>Not swallow exceptions — errors should propagate to the caller</li>
 * </ul>
 */
public interface PolicyEventPublisher {

    /**
     * Publishes a {@link PolicyEvaluatedEvent} when policies have been
     * evaluated against a specification.
     *
     * <p>Consumers: Breaking Change Analysis (contextualizes results),
     * audit logging, and notifications.
     *
     * @param event the event to publish
     */
    void policyEvaluated(PolicyEvaluatedEvent event);

    /**
     * Publishes a {@link PolicySyncedEvent} when policies have been
     * synchronized from an external source.
     *
     * <p>Consumers: Policy cache invalidation, re-evaluation triggers.
     *
     * @param event the event to publish
     */
    void policySynced(PolicySyncedEvent event);

    /**
     * Publishes a {@link PolicySourceChangedEvent} when a policy source
     * configuration has been created, updated, or deleted.
     *
     * <p>Consumers: Connection pool management, configuration reload.
     *
     * @param event the event to publish
     */
    void policySourceChanged(PolicySourceChangedEvent event);
}
