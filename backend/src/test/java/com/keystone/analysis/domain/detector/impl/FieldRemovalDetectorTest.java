package com.keystone.analysis.domain.detector.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.keystone.analysis.domain.model.ChangeSeverity;
import com.keystone.analysis.domain.model.ParsedEndpoint;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FieldRemovalDetectorTest {

    private final FieldRemovalDetector detector = new FieldRemovalDetector();

    @Test
    void detect_shouldReturnBreakingChangeWhenResponseTypeRemoved() {
        var base = new ParsedEndpoint(
                "GET",
                "/api/v1/users",
                "List users",
                Map.of("id", "string"),
                Set.of("User", "application/json"),
                false);
        var target = new ParsedEndpoint(
                "GET", "/api/v1/users", "List users", Map.of("id", "string"), Set.of("application/json"), false);

        var changes = detector.detect(base, target);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).severity()).isEqualTo(ChangeSeverity.BREAKING);
        assertThat(changes.get(0).detectorName()).isEqualTo("FieldRemoval");
    }

    @Test
    void detect_shouldReturnBreakingChangeWhenParameterRemoved() {
        var base = new ParsedEndpoint(
                "GET",
                "/api/v1/users/{id}",
                "Get user",
                Map.of("id", "string", "fields", "string"),
                Set.of("User"),
                false);
        var target = new ParsedEndpoint(
                "GET", "/api/v1/users/{id}", "Get user", Map.of("id", "string"), Set.of("User"), false);

        var changes = detector.detect(base, target);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).path()).contains("parameters.fields");
    }

    @Test
    void detect_shouldReturnEmptyForIdenticalEndpoints() {
        var ep =
                new ParsedEndpoint("GET", "/api/v1/users", "List users", Map.of("id", "string"), Set.of("User"), false);

        var changes = detector.detect(ep, ep);

        assertThat(changes).isEmpty();
    }

    @Test
    void detect_shouldReturnEmptyForNullBase() {
        var target = new ParsedEndpoint("GET", "/api/v1/users", "List users", Map.of(), Set.of("User"), false);

        var changes = detector.detect(null, target);

        assertThat(changes).isEmpty();
    }

    @Test
    void detect_shouldReturnEmptyForNullTarget() {
        var base = new ParsedEndpoint("GET", "/api/v1/users", "List users", Map.of(), Set.of("User"), false);

        var changes = detector.detect(base, null);

        assertThat(changes).isEmpty();
    }
}
