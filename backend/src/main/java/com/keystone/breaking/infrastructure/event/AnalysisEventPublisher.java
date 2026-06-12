package com.keystone.breaking.infrastructure.event;

import com.keystone.breaking.domain.event.AnalysisCompletedEvent;
import com.keystone.breaking.domain.event.BreakingChangesFoundEvent;

/**
 * Outbound port for publishing analysis-related domain events.
 *
 * <p>Per ADR-003 (Event-Driven Communication), analysis events are published
 * via the event bus and also recorded as immutable audit events. The initial
 * implementation uses Spring's {@code ApplicationEventPublisher}
 * (in-process), with the option to swap for Redis Streams or Kafka later.
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
     * Publishes an {@link AnalysisCompletedEvent} when diff analysis completes.
     *
     * <p>Consumers: dashboard (updates analysis history), notification engine,
     * policy engine (triggers policy evaluation).
     *
     * @param event the event to publish
     */
    void analysisCompleted(AnalysisCompletedEvent event);

    /**
     * Publishes a {@link BreakingChangesFoundEvent} when one or more breaking changes
     * are detected.
     *
     * <p>Consumers: notification engine (alerts users), CI integration (blocks merges),
     * dashboard (highlights breaking changes).
     *
     * @param event the event to publish
     */
    void breakingChangesFound(BreakingChangesFoundEvent event);
}
