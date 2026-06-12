package com.keystone.graph.application.dto;

import com.keystone.graph.domain.model.ImpactAnalysisResult;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Output DTO for the impact analysis endpoint.
 *
 * <p>Returned by the {@link com.keystone.graph.interfaces.http.GraphController}
 * after computing the blast radius of a breaking change.
 *
 * @param reportId          The ID of the breaking change report that triggered the analysis
 * @param specPath          The spec path that was analysed
 * @param totalAffected     Total number of affected downstream services
 * @param affectedServices  The list of affected services with impact details
 * @param completedAt       Timestamp of when the analysis completed
 */
public record ImpactAnalysisResponse(
        UUID reportId,
        String specPath,
        int totalAffected,
        List<ImpactAnalysisResult.AffectedService> affectedServices,
        Instant completedAt) {
    public ImpactAnalysisResponse {
        Objects.requireNonNull(reportId, "reportId must not be null");
        Objects.requireNonNull(specPath, "specPath must not be null");
        Objects.requireNonNull(affectedServices, "affectedServices must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
    }

    public List<ImpactAnalysisResult.AffectedService> getAffectedServices() {
        return Collections.unmodifiableList(affectedServices);
    }
}
