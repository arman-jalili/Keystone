// Canonical Reference: .pi/architecture/modules/breaking-change-analysis.md
// Module: breaking-change-analysis
package com.keystone.analysis.infrastructure.repository;

import com.keystone.analysis.domain.model.BreakingChangeReport;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link ChangeReportRepository}.
 *
 * <p>Provides a working implementation for development and testing.
 * A production implementation using Spring Data JPA should replace this.
 */
@Repository
public class ChangeReportRepositoryImpl implements ChangeReportRepository {

    private final Map<UUID, BreakingChangeReport> store = new ConcurrentHashMap<>();

    @Override
    public Optional<BreakingChangeReport> findById(UUID reportId) {
        return Optional.ofNullable(store.get(reportId));
    }

    @Override
    public Optional<BreakingChangeReport> findLatestByRepositoryAndSpecPath(String repository, String specPath) {
        return store.values().stream()
                .filter(r ->
                        r.getRepository().equals(repository) && r.getSpecPath().equals(specPath))
                .max(Comparator.comparing(BreakingChangeReport::getCompletedAt));
    }

    @Override
    public List<BreakingChangeReport> findByRepository(String repository, int page, int pageSize) {
        return store.values().stream()
                .filter(r -> r.getRepository().equals(repository))
                .sorted(Comparator.comparing(BreakingChangeReport::getCompletedAt)
                        .reversed())
                .skip((long) page * pageSize)
                .limit(pageSize)
                .toList();
    }

    @Override
    public BreakingChangeReport save(BreakingChangeReport report) {
        store.put(report.getId(), report);
        return report;
    }

    @Override
    public List<BreakingChangeReport> findLatestReports(int limit) {
        return store.values().stream()
                .sorted(Comparator.comparing(BreakingChangeReport::getCompletedAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public int deleteOlderThan(int retentionDays) {
        Instant cutoff = Instant.now().minus(java.time.Duration.ofDays(retentionDays));
        int before = store.size();
        store.values().removeIf(r -> r.getCompletedAt().isBefore(cutoff));
        return before - store.size();
    }
}
