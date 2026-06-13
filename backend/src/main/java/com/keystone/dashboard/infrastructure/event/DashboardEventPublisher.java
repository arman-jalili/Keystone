// Canonical Reference: .pi/architecture/modules/dashboard.md#data-flow
// Implements: Outbound event publisher for dashboard domain events
package com.keystone.dashboard.infrastructure.event;

import com.keystone.dashboard.domain.event.DashboardViewAccessedEvent;
import com.keystone.dashboard.domain.event.HealthScoreRecalculatedEvent;
import com.keystone.dashboard.domain.event.PolicyStatusChangedEvent;

/**
 * Outbound port for publishing dashboard-related domain events.
 *
 * <p>Per the event-driven architecture, dashboard events are published
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
public interface DashboardEventPublisher {

    /**
     * Publishes a {@link HealthScoreRecalculatedEvent} when a health score
     * has been recomputed.
     *
     * <p>Consumers: Dashboard (cached score invalidation), notification
     * engine (threshold alerts), analytics.
     *
     * @param event the event to publish
     */
    void healthScoreRecalculated(HealthScoreRecalculatedEvent event);

    /**
     * Publishes a {@link DashboardViewAccessedEvent} when a user views
     * a dashboard page.
     *
     * <p>Consumers: Analytics (usage tracking), caching layer.
     *
     * @param event the event to publish
     */
    void dashboardViewAccessed(DashboardViewAccessedEvent event);

    /**
     * Publishes a {@link PolicyStatusChangedEvent} when a policy's lifecycle
     * status changes.
     *
     * <p>Consumers: Policy UI (real-time update), notification engine.
     *
     * @param event the event to publish
     */
    void policyStatusChanged(PolicyStatusChangedEvent event);
}
