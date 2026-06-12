package com.keystone.ingestion.infrastructure.repository;

import com.keystone.ingestion.infrastructure.repository.jpa.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link IdempotencyKeyEntity}.
 */
@Repository
public interface SpringDataIdempotencyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {

    Optional<IdempotencyKeyEntity> findByRepositoryAndCommitShaAndSpecPath(
            String repository, String commitSha, String specPath);

    int deleteByCreatedAtBefore(Instant cutoff);
}
