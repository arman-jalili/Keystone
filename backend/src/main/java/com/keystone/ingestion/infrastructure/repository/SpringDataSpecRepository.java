package com.keystone.ingestion.infrastructure.repository;

import com.keystone.ingestion.infrastructure.repository.jpa.OpenApiSpecEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link OpenApiSpecEntity}.
 */
@Repository
public interface SpringDataSpecRepository extends JpaRepository<OpenApiSpecEntity, UUID> {

    Optional<OpenApiSpecEntity> findByRepositoryAndSpecPath(String repository, String specPath);
}
