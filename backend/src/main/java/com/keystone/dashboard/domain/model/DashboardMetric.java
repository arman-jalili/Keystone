package com.keystone.dashboard.domain.model;

import java.util.Objects;

/**
 * Domain model representing a single KPI metric displayed on the dashboard.
 *
 * <p>Metrics are computed values derived from the underlying data (e.g.
 * "Total specs audited", "Policies passing", "Avg resolution time").
 *
 * @param name        Machine-readable metric identifier
 * @param displayName Human-readable display label
 * @param value       The current numeric value of the metric
 * @param unit        Optional unit suffix (e.g. "ms", "%", "specs")
 * @param change      Period-over-period change, if applicable
 */
public record DashboardMetric(String name, String displayName, double value, String unit, MetricChange change) {

    public DashboardMetric {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
    }

    /**
     * Period-over-period change for a metric.
     *
     * @param direction     Whether the change is positive, negative, or flat
     * @param absoluteValue The absolute change value
     * @param percentage    The percentage change (e.g. 12.5 for +12.5%)
     */
    public record MetricChange(ChangeDirection direction, double absoluteValue, double percentage) {
        public MetricChange {
            Objects.requireNonNull(direction, "direction must not be null");
        }

        public enum ChangeDirection {
            POSITIVE,
            NEGATIVE,
            FLAT
        }
    }
}
