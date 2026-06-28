// Canonical Reference: .pi/architecture/modules/dashboard.md#audit-log-service
package com.keystone.dashboard.infrastructure.repository;

import com.keystone.dashboard.infrastructure.repository.jpa.AuditEntryEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AuditEntryEntity}.
 */
@Repository
public interface SpringDataAuditEntryRepository extends JpaRepository<AuditEntryEntity, String> {

    @Query("SELECT a FROM AuditEntryEntity a ORDER BY a.timestamp DESC")
    List<AuditEntryEntity> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT a FROM AuditEntryEntity a WHERE a.action = :action ORDER BY a.timestamp DESC")
    List<AuditEntryEntity> findByActionOrderByTimestampDesc(@Param("action") String action, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditEntryEntity a")
    long countAll();

    @Query("SELECT COUNT(a) FROM AuditEntryEntity a WHERE a.action = :action")
    long countByAction(@Param("action") String action);
}
