package com.keystone.ingestion.infrastructure.event;

import com.keystone.ingestion.domain.model.IdempotencyKey;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for persisting and querying idempotency keys.
 *
 * <p>Per ADR-007, idempotency keys have a 7-day TTL. Implementations
 * should provide a background cleanup job for expired keys.
 *
 * <p>This is a separate store from {@link com.keystone.ingestion.infrastructure.repository.SpecRepository}
 * because idempotency keys are a cross-cutting concern with different
 * lifecycle and performance requirements (sub-5ms lookup p99).
 */
public interface IdempotencyStore {

    /**
     * Checks whether the given idempotency key has already been recorded.
     *
     * @param key the composite idempotency key
     * @return the associated event ID if the key exists, or empty if not
     */
    Optional<UUID> findEventIdByKey(IdempotencyKey key);

    /**
     * Records an idempotency key with its associated event ID.
     *
     * <p>If a row with this key already exists (concurrent insert), the
     * implementation must handle the conflict gracefully and return
     * the existing event ID.
     *
     * @param key     the composite idempotency key
     * @param eventId the event ID to associate
     * @return the event ID that was recorded (may differ from {@code eventId}
     *         if another process recorded it first)
     */
    UUID save(IdempotencyKey key, UUID eventId);

    /**
     * Deletes idempotency keys older than the given timestamp.
     *
     * <p>Called by the TTL cleanup job to prevent unbounded storage growth.
     *
     * @param cutoff keys older than this timestamp will be deleted
     * @return the number of deleted rows
     */
    int deleteOlderThan(java.time.Instant cutoff);
}
