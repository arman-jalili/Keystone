// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.repository;

import com.keystone.ingestion.infrastructure.repository.jpa.OpenApiSpecEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link OpenApiSpecEntity}.
 */
@Repository
public interface SpringDataSpecRepository extends JpaRepository<OpenApiSpecEntity, UUID> {

    Optional<OpenApiSpecEntity> findByRepositoryAndSpecPath(String repository, String specPath);
}
