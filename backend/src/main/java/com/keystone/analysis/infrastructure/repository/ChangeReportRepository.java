// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.infrastructure.repository;

import com.keystone.analysis.domain.model.BreakingChangeReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for persisting and querying {@link BreakingChangeReport} entities.
 *
 * <p>This is the data access contract. The implementation may use Spring Data JPA,
 * raw JDBC, or any other persistence mechanism. Callers must not depend on
 * implementation details such as table names or column mappings.
 */
public interface ChangeReportRepository {

    /**
     * Finds a report by its unique identifier.
     */
    Optional<BreakingChangeReport> findById(UUID reportId);

    /**
     * Finds the most recent report for a given repository and spec path.
     */
    Optional<BreakingChangeReport> findLatestByRepositoryAndSpecPath(String repository, String specPath);

    /**
     * Finds all reports for a given repository, ordered by completion timestamp descending.
     */
    List<BreakingChangeReport> findByRepository(String repository, int page, int pageSize);

    /**
     * Saves a breaking change report.
     *
     * @param report the report to save
     * @return the saved report with generated fields populated
     */
    BreakingChangeReport save(BreakingChangeReport report);

    /**
     * Deletes reports older than the given retention period.
     *
     * @param retentionDays reports older than this many days will be deleted
     * @return the number of deleted rows
     */
    int deleteOlderThan(int retentionDays);
}
