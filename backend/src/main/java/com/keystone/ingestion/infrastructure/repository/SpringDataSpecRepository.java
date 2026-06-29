// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.repository;

import com.keystone.ingestion.infrastructure.repository.jpa.OpenApiSpecEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link OpenApiSpecEntity}.
 */
@Repository
public interface SpringDataSpecRepository extends JpaRepository<OpenApiSpecEntity, UUID> {

    Optional<OpenApiSpecEntity> findByRepositoryAndSpecPath(String repository, String specPath);

    List<OpenApiSpecEntity> findAllByOrderByIngestedAtDesc();

    @Query("SELECT s FROM OpenApiSpecEntity s WHERE s.id IN "
            + "(SELECT v.specId FROM SpecVersionEntity v GROUP BY v.specId "
            + "HAVING MAX(v.ingestedAt) < :threshold)")
    List<OpenApiSpecEntity> findStaleSpecs(@Param("threshold") Instant threshold);
}
