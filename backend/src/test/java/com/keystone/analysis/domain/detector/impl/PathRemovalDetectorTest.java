package com.keystone.analysis.domain.detector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.analysis.domain.model.ChangeSeverity;
import com.keystone.analysis.domain.model.ParsedEndpoint;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PathRemovalDetectorTest {

    private final PathRemovalDetector detector = new PathRemovalDetector();

    @Test
    void detect_shouldReportBreakingWhenTargetIsNull() {
        var base = new ParsedEndpoint(
                "DELETE", "/api/v1/users/{id}", "Delete user", Map.of("id", "string"), Set.of(), false);

        var changes = detector.detect(base, null);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).severity()).isEqualTo(ChangeSeverity.BREAKING);
        assertThat(changes.get(0).message()).contains("removed");
    }

    @Test
    void detect_shouldReturnEmptyForNullBase() {
        var changes = detector.detect(null, null);
        assertThat(changes).isEmpty();
    }

    @Test
    void detect_shouldReturnEmptyWhenEndpointExists() {
        var ep = new ParsedEndpoint("GET", "/api/v1/users", "List users", Map.of(), Set.of(), false);

        var changes = detector.detect(ep, ep);

        assertThat(changes).isEmpty();
    }
}
