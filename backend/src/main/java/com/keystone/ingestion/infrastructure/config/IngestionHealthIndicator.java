// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.config;

import com.keystone.ingestion.infrastructure.repository.SpecRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the ingestion module.
 *
 * <p>Verifies database connectivity by performing a lightweight query
 * through the SpecRepository. Exposes ingestion-specific metrics.
 */
@Component
public class IngestionHealthIndicator implements HealthIndicator {

    private final SpecRepository specRepository;
    private final Timer healthCheckTimer;

    public IngestionHealthIndicator(SpecRepository specRepository, MeterRegistry meterRegistry) {
        this.specRepository = specRepository;
        this.healthCheckTimer = Timer.builder("ingestion.health.check.duration")
                .description("Time taken for ingestion health check")
                .register(meterRegistry);
    }

    @Override
    public Health health() {
        long start = System.nanoTime();
        try {
            // Lightweight connectivity check
            specRepository.findByRepositoryAndSpecPath("healthcheck", "healthcheck.yaml");
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
