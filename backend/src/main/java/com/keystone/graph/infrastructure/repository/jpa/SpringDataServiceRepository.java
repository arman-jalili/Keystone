// Canonical Reference: .pi/architecture/modules/dependency-graph.md
// Module: dependency-graph
package com.keystone.graph.infrastructure.repository.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link ServiceEntity}.
 */
@Repository
public interface SpringDataServiceRepository extends JpaRepository<ServiceEntity, UUID> {

    Optional<ServiceEntity> findByName(String name);

    @Query("SELECT s FROM ServiceEntity s WHERE s.id IN "
            + "(SELECT d.producerId FROM ApiDependencyEntity d WHERE d.specPath = :specPath)")
    List<ServiceEntity> findProducersBySpecPath(@Param("specPath") String specPath);
}
