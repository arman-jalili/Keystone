// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.event;

import com.keystone.ingestion.domain.event.SpecIngestedEvent;
import com.keystone.ingestion.domain.event.SpecParseFailedEvent;

/**
 * Outbound port for publishing ingestion-related domain events.
 *
 * <p>Per ADR-003 and ADR-004, ingestion events are published via the
 * event bus and also recorded as immutable audit events. The initial
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
public interface IngestionEventPublisher {

    /**
     * Publishes a {@link SpecIngestedEvent} when a spec has been successfully ingested.
     *
     * <p>Consumers: Breaking Change Analysis (triggers diff analysis).
     *
     * @param event the event to publish
     */
    void specIngested(SpecIngestedEvent event);

    /**
     * Publishes a {@link SpecParseFailedEvent} when a spec fails validation or parsing.
     *
     * <p>Consumers: audit logging, monitoring, and alerting.
     *
     * @param event the event to publish
     */
    void specParseFailed(SpecParseFailedEvent event);
}
