package com.keystone.analysis.domain.detector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.analysis.domain.model.ChangeSeverity;
import com.keystone.analysis.domain.model.ParsedEndpoint;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeprecatedFieldDetectorTest {

    private final DeprecatedFieldDetector detector = new DeprecatedFieldDetector();

    @Test
    void detect_shouldReportDeprecationWhenEndpointBecomesDeprecated() {
        var base = new ParsedEndpoint("GET", "/api/v1/users/{id}", "Get user", Map.of(), Set.of(), false);
        var target = new ParsedEndpoint("GET", "/api/v1/users/{id}", "Get user", Map.of(), Set.of(), true);

        var changes = detector.detect(base, target);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).severity()).isEqualTo(ChangeSeverity.DEPRECATION);
        assertThat(changes.get(0).message()).contains("now deprecated");
    }

    @Test
    void detect_shouldReturnEmptyWhenAlreadyDeprecated() {
        var ep = new ParsedEndpoint("GET", "/api/v1/old", "Old endpoint", Map.of(), Set.of(), true);

        assertThat(detector.detect(ep, ep)).isEmpty();
    }

    @Test
    void detect_shouldReturnEmptyForNull() {
        assertThat(detector.detect(null, null)).isEmpty();
    }
}
