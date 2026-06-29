package com.keystone.analysis.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.keystone.analysis.domain.detector.impl.PathRemovalDetector;
import com.keystone.analysis.domain.model.BaseVersion;
import com.keystone.analysis.domain.model.ChangeSeverity;
import com.keystone.analysis.domain.model.ParsedEndpoint;
import com.keystone.analysis.domain.model.Verdict;
import com.keystone.analysis.domain.service.DetectorRegistry;
import com.keystone.analysis.domain.service.SpecParser;
import com.keystone.analysis.infrastructure.event.AnalysisEventPublisher;
import com.keystone.analysis.infrastructure.repository.ChangeReportRepository;
import com.keystone.ingestion.domain.model.SpecVersion;
import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class DiffOrchestratorImplTest {

    @Mock
    private DetectorRegistry detectorRegistry;

    @Mock
    private SpecRepository specRepository;

    @Mock
    private SpecParser specParser;

    @Mock
    private ChangeReportRepository reportRepository;

    @Mock
    private AnalysisEventPublisher eventPublisher;

    private DiffOrchestratorImpl orchestrator;

    private final UUID specId = UUID.randomUUID();
    private final String repo = "org/repo";
    private final String specPath = "openapi/api.yaml";

    @BeforeEach
    void setUp() {
        orchestrator = new DiffOrchestratorImpl(
                detectorRegistry, null, reportRepository, eventPublisher, specRepository, specParser);
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void analyzeWithBase_shouldDetectBreakingChanges() {
        var baseEndpoint =
                new ParsedEndpoint("GET", "/api/v1/users", "List users", Map.of("id", "string"), Set.of("User"), false);
        var baseVersion = new BaseVersion(UUID.randomUUID().toString(), "test", "v1", Instant.now());

        mockVersion(baseVersion.versionId(), "openapi v1 content");
        mockVersion(specId.toString(), "openapi v2 content");
        when(specParser.parse("openapi v1 content")).thenReturn(List.of(baseEndpoint));
        when(specParser.parse("openapi v2 content")).thenReturn(List.of());

        var detector = new PathRemovalDetector();
        when(detectorRegistry.getAllDetectors()).thenReturn(List.of(detector));

        var report = orchestrator.analyzeWithBase(repo, specPath, specId, baseVersion);

        assertThat(report.getVerdict()).isEqualTo(Verdict.BREAKING);
        assertThat(report.getChanges()).isNotEmpty();
        assertThat(report.getChanges().get(0).severity()).isEqualTo(ChangeSeverity.BREAKING);
    }

    @Test
    void analyzeWithBase_shouldPassWhenNoChanges() {
        var ep = new ParsedEndpoint("GET", "/api/v1/users", "List users", Map.of(), Set.of(), false);
        var baseVersion = new BaseVersion(UUID.randomUUID().toString(), "test", "v1", Instant.now());

        mockVersion(baseVersion.versionId(), "same content");
        mockVersion(specId.toString(), "same content");
        when(specParser.parse("same content")).thenReturn(List.of(ep));

        var detector = new PathRemovalDetector();
        when(detectorRegistry.getAllDetectors()).thenReturn(List.of(detector));

        var report = orchestrator.analyzeWithBase(repo, specPath, specId, baseVersion);

        assertThat(report.getVerdict()).isEqualTo(Verdict.PASS);
        assertThat(report.getChanges()).isEmpty();
    }

    private void mockVersion(String versionId, String content) {
        var version = new SpecVersion(UUID.fromString(versionId), specId, "sha", "checksum", content, Instant.now());
        when(specRepository.findVersionById(UUID.fromString(versionId))).thenReturn(Optional.of(version));
        // fetchLatestVersion calls findVersionsBySpecId with the target specId
        when(specRepository.findVersionsBySpecId(specId, 1)).thenReturn(List.of(version));
    }
}
