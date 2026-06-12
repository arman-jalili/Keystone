package com.keystone.graph.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a service's dependency declarations have been registered.
 *
 * <p>Published by {@link com.keystone.graph.domain.service.DependencyParser} after
 * successfully processing a {@code keystone.yml} declaration and registering
 * {@link com.keystone.graph.domain.model.Service} nodes and
 * {@link com.keystone.graph.domain.model.ApiDependency} edges.
 *
 * <p>Consumers: Dashboard (updates service registry view),
 * Audit (records registration event).
 *
 * @param eventId        Unique identifier for this event occurrence
 * @param serviceName    The name of the registered service
 * @param producerCount  Number of produced API specs registered
 * @param consumerCount  Number of consumed API specs registered
 * @param timestamp      ISO-8601 timestamp of when the registration occurred
 * @param idempotencyKey Deduplication key for this event
 */
public record DependencyAddedEvent(
    UUID eventId,
    String serviceName,
    int producerCount,
    int consumerCount,
    Instant timestamp,
    String idempotencyKey
) implements DomainEvent<DependencyAddedEvent.Payload> {

    public DependencyAddedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        if (producerCount < 0) throw new IllegalArgumentException("producerCount must not be negative");
        if (consumerCount < 0) throw new IllegalArgumentException("consumerCount must not be negative");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    @Override
    public UUID getEventId() { return eventId; }

    @Override
    public String getEventType() { return "DependencyAdded"; }

    @Override
    public String getSource() { return "dependency-graph"; }

    @Override
    public Instant getTimestamp() { return timestamp; }

    @Override
    public Payload getPayload() {
        return new Payload(serviceName, producerCount, consumerCount);
    }

    @Override
    public String getIdempotencyKey() { return idempotencyKey; }

    /**
     * The data payload carried by a DependencyAdded event.
     */
    public record Payload(
        String serviceName,
        int producerCount,
        int consumerCount
    ) {}
}
