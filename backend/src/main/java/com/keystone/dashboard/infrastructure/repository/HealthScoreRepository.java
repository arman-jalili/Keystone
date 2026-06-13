// Canonical Reference: .pi/architecture/modules/dashboard.md#health-score-service
// Implements: Repository interface for health score data access
package com.keystone.dashboard.infrastructure.repository;

import com.keystone.dashboard.domain.model.HealthScore;
import com.keystone.dashboard.domain.model.HealthTrend;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing {@link HealthScore} data.
 *
 * <p>This is the data access contract for health scores. The implementation
 * may use Spring Data JPA, JDBC, or any other persistence mechanism.
 * Callers must not depend on implementation details such as table names
 * or column mappings.
 */
public interface HealthScoreRepository {

    /**
     * Finds the latest health score for a given entity.
     *
     * @param entityType the type of entity
     * @param entityId   the entity identifier
     * @return the latest health score if one exists
     */
    Optional<HealthScore> findLatestByEntity(String entityType, String entityId);

    /**
     * Finds the health score trend for a given entity over time.
     *
     * @param entityType the type of entity
     * @param entityId   the entity identifier
     * @param limit      maximum number of data points to return
     * @return the health trend with ordered data points
     */
    HealthTrend findTrendByEntity(String entityType, String entityId, int limit);

    /**
     * Finds all latest health scores, grouped by entity type.
     *
     * @return list of the latest health scores for each tracked entity
     */
    List<HealthScore> findAllLatest();

    /**
     * Persists a new health score record.
     *
     * @param score the health score to save
     * @return the saved health score with any generated fields populated
     */
    HealthScore save(HealthScore score);
}
