package com.keystone.analysis.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keystone.analysis.domain.model.BreakingChangeReport;
import com.keystone.analysis.domain.model.Change;
import com.keystone.analysis.domain.model.ChangeSeverity;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Response DTO returned when a breaking change analysis has completed.
 *
 * @param analysisId    Unique identifier for this analysis
 * @param repository    The repository that was analysed
 * @param specPath      The spec path that was analysed
 * @param baseVersion   The spec version used as the baseline for comparison
 * @param targetVersion The new spec version that was analysed
 * @param verdict       The overall analysis verdict (PASS, BREAKING, NON_BREAKING, INCONCLUSIVE)
 * @param changes       The list of all detected changes
 * @param breakingCount Number of BREAKING-severity changes
 * @param totalChanges  Total number of changes detected
 * @param completedAt   ISO-8601 timestamp of analysis completion
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisResponse(
    UUID analysisId,
    String repository,
    String specPath,
    String baseVersion,
    String targetVersion,
    String verdict,
    List<ChangeDetail> changes,
    int breakingCount,
    int totalChanges,
    Instant completedAt
) {
    public AnalysisResponse {
        Objects.requireNonNull(analysisId, "analysisId must not be null");
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(targetVersion, "targetVersion must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(changes, "changes must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    /**
     * Factory method to create an AnalysisResponse from a domain {@link BreakingChangeReport}.
     */
    public static AnalysisResponse from(BreakingChangeReport report) {
        List<ChangeDetail> details = report.getChanges().stream()
                .map(c -> new ChangeDetail(
                        c.id(),
                        c.severity().name(),
                        c.path(),
                        c.oldValue(),
                        c.newValue(),
                        c.message(),
                        c.detectorName()))
                .toList();

        int breakingCount = (int) report.getChanges().stream()
                .filter(c -> c.severity() == ChangeSeverity.BREAKING)
                .count();

        return new AnalysisResponse(
                report.getId(),
                report.getRepository(),
                report.getSpecPath(),
                report.getBaseVersion(),
                report.getTargetVersion(),
                report.getVerdict().name(),
                details,
                breakingCount,
                report.getChanges().size(),
                report.getCompletedAt());
    }
}
