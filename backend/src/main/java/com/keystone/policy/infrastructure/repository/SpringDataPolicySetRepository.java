package com.keystone.policy.infrastructure.repository;

import com.keystone.policy.infrastructure.repository.jpa.PolicySetEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link PolicySetEntity}.
 */
@Repository
public interface SpringDataPolicySetRepository extends JpaRepository<PolicySetEntity, UUID> {

    Optional<PolicySetEntity> findByName(String name);
}
