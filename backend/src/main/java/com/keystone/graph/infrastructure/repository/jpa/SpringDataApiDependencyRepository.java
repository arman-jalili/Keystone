package com.keystone.graph.infrastructure.repository.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link ApiDependencyEntity}.
 */
@Repository
public interface SpringDataApiDependencyRepository extends JpaRepository<ApiDependencyEntity, UUID> {

    List<ApiDependencyEntity> findByProducerId(UUID producerId);

    List<ApiDependencyEntity> findByConsumerId(UUID consumerId);

    List<ApiDependencyEntity> findByProducerIdIn(List<UUID> producerIds);

    void deleteByProducerIdOrConsumerId(UUID producerId, UUID consumerId);
}
