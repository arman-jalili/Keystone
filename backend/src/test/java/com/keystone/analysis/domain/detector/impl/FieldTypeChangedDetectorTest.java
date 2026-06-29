package com.keystone.analysis.domain.detector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.analysis.domain.model.ChangeSeverity;
import com.keystone.analysis.domain.model.ParsedEndpoint;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FieldTypeChangedDetectorTest {

    private final FieldTypeChangedDetector detector = new FieldTypeChangedDetector();

    @Test
    void detect_shouldReportBreakingWhenParameterTypeChanges() {
        var base = new ParsedEndpoint(
                "GET", "/api/v1/users/{id}", "Get user", Map.of("id", "integer"), Set.of("User"), false);
        var target = new ParsedEndpoint(
                "GET", "/api/v1/users/{id}", "Get user", Map.of("id", "string"), Set.of("User"), false);

        var changes = detector.detect(base, target);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).severity()).isEqualTo(ChangeSeverity.BREAKING);
    }

    @Test
    void detect_shouldReturnEmptyForSameTypes() {
        var ep =
                new ParsedEndpoint("GET", "/api/v1/users", "List users", Map.of("id", "string"), Set.of("User"), false);

        var changes = detector.detect(ep, ep);

        assertThat(changes).isEmpty();
    }

    @Test
    void detect_shouldReturnEmptyForNull() {
        assertThat(detector.detect(null, null)).isEmpty();
        assertThat(detector.detect(null, new ParsedEndpoint("GET", "/", "", Map.of(), Set.of(), false)))
                .isEmpty();
    }
}
