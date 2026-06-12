package com.keystone.graph.infrastructure.event;

import com.keystone.graph.domain.event.DependencyAddedEvent;
import com.keystone.graph.domain.event.DownstreamImpactComputedEvent;

/**
 * Outbound port for publishing dependency graph-related domain events.
 *
 * <p>Per ADR-003 (Event-Driven Communication), graph events are published
 * via Spring's {@code ApplicationEventPublisher} (in-process), with the
 * option to swap for Redis Streams or Kafka later.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>Publish events synchronously or asynchronously (configurable)</li>
 *   <li>Ensure at-least-once delivery semantics</li>
 *   <li>Not swallow exceptions — errors should propagate to the caller</li>
 * </ul>
 */
public interface GraphEventPublisher {

    /**
     * Publishes a {@link DependencyAddedEvent} when a service's dependencies
     * are registered.
     *
     * <p>Consumers: Dashboard (updates service registry view),
     * Audit (records registration event).
     *
     * @param event the event to publish
     */
    void dependencyAdded(DependencyAddedEvent event);

    /**
     * Publishes a {@link DownstreamImpactComputedEvent} when impact analysis
     * completes.
     *
     * <p>Consumers: Notification Engine (alerts affected service teams),
     * Dashboard (updates impact view),
     * Audit (records impact analysis event).
     *
     * @param event the event to publish
     */
    void downstreamImpactComputed(DownstreamImpactComputedEvent event);
}
