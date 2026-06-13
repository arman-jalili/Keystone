package com.keystone.dashboard.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing a time range for dashboard queries.
 *
 * <p>Supports both predefined ranges (e.g. LAST_7_DAYS) and custom
 * start/end boundaries. Ranges are always normalized to UTC.
 *
 * @param type  The type of time range (predefined or custom)
 * @param start Start of the range (inclusive), UTC
 * @param end   End of the range (exclusive), UTC
 */
public record TimeRange(RangeType type, Instant start, Instant end) {

    public TimeRange {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }

    /**
     * Creates a predefined time range of the given type, relative to now.
     */
    public static TimeRange lastXDays(RangeType type, Instant now) {
        Duration duration = switch (type) {
            case LAST_24_HOURS -> Duration.ofHours(24);
            case LAST_7_DAYS -> Duration.ofDays(7);
            case LAST_30_DAYS -> Duration.ofDays(30);
            case LAST_90_DAYS -> Duration.ofDays(90);
            case CUSTOM -> throw new IllegalArgumentException("Use constructor for custom ranges");
        };
        return new TimeRange(type, now.minus(duration), now);
    }

    /**
     * Predefined time range types.
     */
    public enum RangeType {
        LAST_24_HOURS,
        LAST_7_DAYS,
        LAST_30_DAYS,
        LAST_90_DAYS,
        CUSTOM
    }
}
