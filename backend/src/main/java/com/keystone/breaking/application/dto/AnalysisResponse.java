package com.keystone.breaking.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keystone.breaking.domain.model.DiffReport;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Response DTO returned when a breaking change analysis has completed.
 *
 * <p>Contains the full analysis summary including detected changes,
 * version information, and analysis metadata.
 *
 * @param analysisId     Unique identifier for this analysis
 * @param repository     The repository that was analysed
 * @param specPath       The spec path that was analysed
 * @param baseVersion    The spec version used as the baseline for comparison
 * @param newVersion     The new spec version that was analysed
 * @param status         The overall analysis status
 * @param changes        The list of all detected changes
 * @param breakingCount  Number of BREAKING-severity changes
 * @param warningCount   Number of WARNING-severity changes
 * @param infoCount      Number of INFO-severity changes
 * @param completedAt    ISO-8601 timestamp of analysis completion
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisResponse(
    UUID analysisId,
    String repository,
    String specPath,
    String baseVersion,
    String newVersion,
    String status,
    List<BreakingChangeDetail> changes,
    int breakingCount,
    int warningCount,
    int infoCount,
    Instant completedAt
) {
    public AnalysisResponse {
        Objects.requireNonNull(analysisId, "analysisId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(newVersion, "newVersion must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(changes, "changes must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    /**
     * Factory method to create an AnalysisResponse from a domain {@link DiffReport}.
     *
     * @param report the completed diff report
     * @return a response DTO suitable for serialization
     */
    public static AnalysisResponse from(DiffReport report) {
        List<BreakingChangeDetail> details = report.getChanges().stream()
                .map(bc -> new BreakingChangeDetail(
                        bc.id(),
                        bc.type().getCode(),
                        bc.severity().name(),
                        bc.path(),
                        bc.oldValue(),
                        bc.newValue(),
                        bc.description()))
                .toList();

        int breakingCount = (int) report.getChanges().stream()
                .filter(c -> c.severity() == com.keystone.breaking.domain.model.BreakingChange.Severity.BREAKING)
                .count();
        int warningCount = (int) report.getChanges().stream()
                .filter(c -> c.severity() == com.keystone.breaking.domain.model.BreakingChange.Severity.WARNING)
                .count();
        int infoCount = (int) report.getChanges().stream()
                .filter(c -> c.severity() == com.keystone.breaking.domain.model.BreakingChange.Severity.INFO)
                .count();

        return new AnalysisResponse(
                report.getAnalysisId(),
                report.getRepository(),
                report.getSpecPath(),
                report.getBaseVersion(),
                report.getNewVersion(),
                report.getStatus().name(),
                details,
                breakingCount,
                warningCount,
                infoCount,
                report.getCompletedAt());
    }
}
