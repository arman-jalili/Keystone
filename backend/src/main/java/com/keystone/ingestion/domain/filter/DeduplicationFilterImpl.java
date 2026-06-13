// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.domain.filter;

import com.keystone.ingestion.domain.model.IdempotencyKey;
import com.keystone.ingestion.infrastructure.event.IdempotencyStore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Checks and marks idempotency keys for deduplication.
 *
 * <p>Per ADR-007, deduplication is enforced by the composite key
 * {@code (repository, commitSha, specPath)} backed by the {@link IdempotencyStore}.
 */
@Component
public class DeduplicationFilterImpl implements DeduplicationFilter {

    private final IdempotencyStore idempotencyStore;

    public DeduplicationFilterImpl(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public Optional<UUID> isDuplicate(IdempotencyKey key) {
        return idempotencyStore.findEventIdByKey(key);
    }

    @Override
    public UUID markProcessed(IdempotencyKey key, UUID eventId) {
        return idempotencyStore.save(key, eventId);
    }
}
