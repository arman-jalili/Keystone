// Canonical Reference: .pi/architecture/modules/dependency-graph.md#health-check
// Implements: GraphHealthIndicator
// Issue: #80
package com.keystone.graph.infrastructure.config;

import com.keystone.graph.infrastructure.repository.GraphRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the dependency-graph module.
 *
 * <p>Verifies database connectivity by performing a lightweight query
 * through the GraphRepository. Exposes graph-specific metrics.
 */
@Component
public class GraphHealthIndicator implements HealthIndicator {

    private final GraphRepository graphRepository;
    private final Timer healthCheckTimer;

    public GraphHealthIndicator(GraphRepository graphRepository, MeterRegistry meterRegistry) {
        this.graphRepository = graphRepository;
        this.healthCheckTimer = Timer.builder("graph.health.check.duration")
                .description("Time taken for dependency graph health check")
                .register(meterRegistry);
    }

    @Override
    public Health health() {
        long start = System.nanoTime();
        try {
            // Lightweight connectivity check
            graphRepository.findAllServices();
            long elapsed = System.nanoTime() - start;
            healthCheckTimer.record(elapsed, TimeUnit.NANOSECONDS);
            return Health.up()
                    .withDetail("database", "reachable")
                    .withDetail("latency_ms", TimeUnit.NANOSECONDS.toMillis(elapsed))
                    .build();
        } catch (Exception ex) {
            long elapsed = System.nanoTime() - start;
            healthCheckTimer.record(elapsed, TimeUnit.NANOSECONDS);
            return Health.down(ex)
                    .withDetail("database", "unreachable")
                    .withDetail("latency_ms", TimeUnit.NANOSECONDS.toMillis(elapsed))
                    .build();
        }
    }
}
