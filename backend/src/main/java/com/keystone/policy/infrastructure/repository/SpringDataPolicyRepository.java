package com.keystone.policy.infrastructure.repository;

import com.keystone.policy.infrastructure.repository.jpa.PolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PolicyEntity}.
 */
@Repository
public interface SpringDataPolicyRepository extends JpaRepository<PolicyEntity, UUID> {

    Optional<PolicyEntity> findByName(String name);

    Optional<PolicyEntity> findByNameAndSourceId(String name, String sourceId);

    List<PolicyEntity> findByStatus(String status);

    List<PolicyEntity> findBySourceId(String sourceId);

    List<PolicyEntity> findBySourceIdAndStatus(String sourceId, String status);

    @Modifying
    @Query("UPDATE PolicyEntity p SET p.status = 'INACTIVE' WHERE p.name NOT IN :activeNames")
    int deactivateStalePolicies(@Param("activeNames") List<String> activeNames);
}
