package com.keystone.graph.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the result of an impact analysis.
 *
 * <p>Produced by {@link com.keystone.graph.domain.service.ImpactAnalyzer} after
 * performing a BFS traversal to find all downstream services affected by a
 * breaking change to a specific spec.
 *
 * <p>The result includes the original change that triggered the analysis and
 * the set of affected services, each with an estimated impact severity.
 *
 * <p>This value object is ephemeral (not persisted) — it is published via
 * {@link com.keystone.graph.domain.event.DownstreamImpactComputedEvent} for
 * downstream consumers (e.g., Dashboard, Notification Engine).
 */
public class ImpactAnalysisResult {

    /**
     * Severity of impact on an affected service.
     */
    public enum ImpactSeverity {
        /** The service directly depends on the changed spec. */
        DIRECT,
        /** The service transitively depends on the changed spec. */
        TRANSITIVE,
        /** Impact cannot be determined. */
        UNKNOWN
    }

    private final UUID reportId;
    private final List<AffectedService> affectedServices;

    /**
     * @param reportId          The ID of the breaking change report that triggered this analysis
     * @param affectedServices  The list of affected downstream services
     */
    public ImpactAnalysisResult(UUID reportId, List<AffectedService> affectedServices) {
        this.reportId = Objects.requireNonNull(reportId, "reportId must not be null");
        this.affectedServices = List.copyOf(
                Objects.requireNonNull(affectedServices, "affectedServices must not be null"));
    }

    public UUID getReportId() { return reportId; }

    public List<AffectedService> getAffectedServices() {
        return Collections.unmodifiableList(affectedServices);
    }

    /**
     * Returns true if any services were affected by the change.
     */
    public boolean hasAffectedServices() {
        return !affectedServices.isEmpty();
    }

    /**
     * A single service affected by a breaking change.
     *
     * @param serviceId  The ID of the affected service
     * @param serviceName The name of the affected service
     * @param severity   The estimated impact severity
     * @param path       The dependency path (e.g. "user-svc → payment-svc")
     */
    public record AffectedService(
        UUID serviceId,
        String serviceName,
        ImpactSeverity severity,
        String path
    ) {
        public AffectedService {
            Objects.requireNonNull(serviceId, "serviceId must not be null");
            Objects.requireNonNull(serviceName, "serviceName must not be null");
            Objects.requireNonNull(severity, "severity must not be null");
            Objects.requireNonNull(path, "path must not be null");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImpactAnalysisResult that)) return false;
        return Objects.equals(reportId, that.reportId)
                && Objects.equals(affectedServices, that.affectedServices);
    }

    @Override
    public int hashCode() { return Objects.hash(reportId, affectedServices); }

    @Override
    public String toString() {
        return "ImpactAnalysisResult{reportId=" + reportId
               + ", affectedCount=" + affectedServices.size() + "}";
    }
}
