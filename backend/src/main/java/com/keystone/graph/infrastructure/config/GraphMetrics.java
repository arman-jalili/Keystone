// Canonical Reference: .pi/architecture/modules/dependency-graph.md#metrics
// Implements: GraphMetrics
// Issue: #80
package com.keystone.graph.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Registers dependency-graph-specific Micrometer metrics.
 *
 * <p>These metrics are exposed via {@code /actuator/metrics} and
 * {@code /actuator/prometheus} for monitoring and alerting.
 */
@Component
public class GraphMetrics {

    private final MeterRegistry meterRegistry;

    public GraphMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerMetrics() {
        // Service registration counter
        Counter.builder("graph.registrations.total")
                .description("Total number of service registrations")
                .register(meterRegistry);

        // Duplicate registrations
        Counter.builder("graph.registrations.duplicate")
                .description("Number of duplicate service registrations")
                .register(meterRegistry);

        // Service removal counter
        Counter.builder("graph.removals.total")
                .description("Total number of service removals")
                .register(meterRegistry);

        // Impact analysis counter
        Counter.builder("graph.impact.analyses")
                .description("Number of impact analyses performed")
                .register(meterRegistry);

        // Impact analysis duration
        Timer.builder("graph.impact.duration")
                .description("Time taken for BFS impact analysis")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // Service registration duration
        Timer.builder("graph.registration.time")
                .description("Time taken to register a service declaration")
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Cycle detection counter
        Counter.builder("graph.cycles.detected")
                .description("Number of dependency cycles detected")
                .register(meterRegistry);

        // Graph query latency
        Timer.builder("graph.query.duration")
                .description("Time taken for graph repository queries")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }
}
