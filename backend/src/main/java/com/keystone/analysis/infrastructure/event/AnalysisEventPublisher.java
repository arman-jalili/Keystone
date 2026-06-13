// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.infrastructure.event;

import com.keystone.analysis.domain.event.BreakingChangeReportedEvent;

/**
 * Outbound port for publishing analysis-related domain events.
 *
 * <p>Per ADR-003 (Event-Driven Communication), analysis events are published
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
public interface AnalysisEventPublisher {

    /**
     * Publishes a {@link BreakingChangeReportedEvent} when analysis completes.
     *
     * <p>Consumers: Policy Engine (triggers policy evaluation),
     * Dashboard (updates analysis history),
     * Notification Engine (alerts users of breaking changes).
     *
     * @param event the event to publish
     */
    void breakingChangeReported(BreakingChangeReportedEvent event);
}
