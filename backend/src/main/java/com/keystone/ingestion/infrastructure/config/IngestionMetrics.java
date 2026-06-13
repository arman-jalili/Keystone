// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Registers ingestion-specific Micrometer metrics.
 *
 * <p>These metrics are exposed via {@code /actuator/metrics} and
 * {@code /actuator/prometheus} for monitoring and alerting.
 */
@Component
public class IngestionMetrics {

    private final MeterRegistry meterRegistry;

    public IngestionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerMetrics() {
        // Ingestion request counter
        Counter.builder("ingestion.requests.total")
                .description("Total number of ingestion requests")
                .register(meterRegistry);

        // Duplicate hits (idempotent)
        Counter.builder("ingestion.dedup.hits")
                .description("Number of duplicate ingestion requests")
                .register(meterRegistry);

        // New requests
        Counter.builder("ingestion.dedup.misses")
                .description("Number of new ingestion requests")
                .register(meterRegistry);

        // Validation errors
        Counter.builder("ingestion.validation.errors")
                .description("Number of spec validation failures")
                .register(meterRegistry);

        // Request duration timer
        Timer.builder("ingestion.requests.duration")
                .description("Ingestion request processing time")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // Dedup check latency
        Timer.builder("ingestion.dedup.time")
                .description("Time spent checking idempotency")
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Event publish latency
        Timer.builder("ingestion.event.publish")
                .description("Time spent publishing domain events")
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Spec validation latency
        Timer.builder("ingestion.validation.time")
                .description("Time spent validating OpenAPI specs")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }
}
