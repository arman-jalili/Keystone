package com.keystone.ingestion.domain.filter;

import com.keystone.ingestion.domain.model.IdempotencyKey;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain service interface for deduplication of incoming specs.
 *
 * <p>Per ADR-007, idempotency is enforced by the composite key
 * {@code (repository, commitSha, specPath)}. Implementations must:
 * <ul>
 *   <li>Check if a key has already been processed before allowing ingestion</li>
 *   <li>Mark a key as processed atomically after successful ingestion</li>
 *   <li>Handle concurrent checks safely (use database-level locking or unique constraints)</li>
 * </ul>
 */
public interface DeduplicationFilter {

    /**
     * Checks whether the given idempotency key has already been processed.
     *
     * @param key the composite idempotency key (repository, commitSha, specPath)
     * @return the existing event ID if already processed, or empty if this is a new request
     */
    Optional<UUID> isDuplicate(IdempotencyKey key);

    /**
     * Atomically marks the idempotency key as processed.
     *
     * <p>Must be called within the same transaction that persists the spec.
     * If the key already exists (concurrent race condition), this should
     * return the existing event ID rather than throwing.
     *
     * @param key     the composite idempotency key
     * @param eventId the event ID that processed this key
     * @return the event ID that was recorded (may differ from {@code eventId} if
     *         another process recorded it first)
     */
    UUID markProcessed(IdempotencyKey key, UUID eventId);
}
