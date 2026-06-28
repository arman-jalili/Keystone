// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
package com.keystone.dashboard.infrastructure.repository;

import com.keystone.dashboard.infrastructure.repository.jpa.HealthScoreEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link HealthScoreEntity}.
 */
@Repository
public interface SpringDataHealthScoreRepository extends JpaRepository<HealthScoreEntity, UUID> {

    @Query("SELECT h FROM HealthScoreEntity h WHERE h.entityType = :entityType AND h.entityId = :entityId ORDER BY h.computedAt DESC")
    List<HealthScoreEntity> findByEntityOrderByComputedAtDesc(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            Pageable pageable);
}
