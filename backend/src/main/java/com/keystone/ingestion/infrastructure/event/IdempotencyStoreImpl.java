package com.keystone.ingestion.infrastructure.event;

import com.keystone.ingestion.domain.model.IdempotencyKey;
import com.keystone.ingestion.infrastructure.repository.SpringDataIdempotencyRepository;
import com.keystone.ingestion.infrastructure.repository.jpa.IdempotencyKeyEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores idempotency keys in the database using Spring Data JPA.
 *
 * <p>Per ADR-007, keys have a composite unique constraint on
 * {@code (repository, commitSha, specPath)} and a 7-day TTL.
 */
@Component
public class IdempotencyStoreImpl implements IdempotencyStore {

    private final SpringDataIdempotencyRepository repository;

    public IdempotencyStoreImpl(SpringDataIdempotencyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findEventIdByKey(IdempotencyKey key) {
        return repository
                .findByRepositoryAndCommitShaAndSpecPath(key.repository(), key.commitSha(), key.specPath())
                .map(IdempotencyKeyEntity::getEventId);
    }

    @Override
    @Transactional
    public UUID save(IdempotencyKey key, UUID eventId) {
        var entity =
                new IdempotencyKeyEntity(eventId, key.repository(), key.commitSha(), key.specPath(), Instant.now());
        var saved = repository.save(entity);
        return saved.getEventId();
    }

    @Override
    @Transactional
    public int deleteOlderThan(Instant cutoff) {
        return repository.deleteByCreatedAtBefore(cutoff);
    }
}
