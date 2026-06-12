package com.keystone.breaking.infrastructure.repository;

import com.keystone.breaking.domain.model.DiffReport;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for persisting and querying {@link DiffReport} entities.
 *
 * <p>This is the data access contract. The implementation may use Spring Data JPA,
 * raw JDBC, or any other persistence mechanism. Callers must not depend on
 * implementation details such as table names or column mappings.
 *
 * <p>Reports are stored as immutable documents. Once written, a report should
 * not be modified. Updates are handled by creating new analysis instances
 * (see {@link DiffReport#getAnalysisId()}).
 */
public interface DiffReportRepository {

    /**
     * Finds a diff report by its unique identifier.
     *
     * @param reportId the report UUID
     * @return the report if found, or empty if not
     */
    Optional<DiffReport> findById(UUID reportId);

    /**
     * Finds all diff reports for a given analysis.
     *
     * @param analysisId the analysis UUID
     * @return list of reports, ordered by completion timestamp descending
     */
    List<DiffReport> findByAnalysisId(UUID analysisId);

    /**
     * Finds the most recent diff report for a given repository and spec path.
     *
     * @param repository the repository identifier (e.g. "org/repo")
     * @param specPath   the relative spec path within the repository
     * @return the most recent report if one exists, or empty if not
     */
    Optional<DiffReport> findLatestByRepositoryAndSpecPath(String repository, String specPath);

    /**
     * Finds all diff reports for a given repository, ordered by completion
     * timestamp descending, with pagination.
     *
     * @param repository the repository identifier
     * @param page       zero-based page index
     * @param pageSize   maximum number of results per page
     * @return list of reports
     */
    List<DiffReport> findByRepository(String repository, int page, int pageSize);

    /**
     * Saves a diff report to the data store.
     *
     * <p>Reports are immutable — this method should only be called once
     * per report. Re-saves (updates) should not occur in normal operation.
     *
     * @param report the report to save
     * @return the saved report with any generated fields populated
     */
    DiffReport save(DiffReport report);

    /**
     * Deletes reports older than the given number of days.
     *
     * <p>Called by the retention cleanup job to prevent unbounded storage growth.
     *
     * @param retentionDays reports older than this many days will be deleted
     * @return the number of deleted rows
     */
    int deleteOlderThan(int retentionDays);
}
