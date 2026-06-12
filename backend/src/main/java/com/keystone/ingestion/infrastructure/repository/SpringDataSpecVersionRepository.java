package com.keystone.ingestion.infrastructure.repository;

import com.keystone.ingestion.infrastructure.repository.jpa.SpecVersionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link SpecVersionEntity}.
 */
@Repository
public interface SpringDataSpecVersionRepository extends JpaRepository<SpecVersionEntity, UUID> {

    @Query("SELECT v FROM SpecVersionEntity v WHERE v.specId = :specId ORDER BY v.ingestedAt DESC")
    List<SpecVersionEntity> findBySpecIdOrderByIngestedAtDesc(@Param("specId") UUID specId, Pageable pageable);
}
