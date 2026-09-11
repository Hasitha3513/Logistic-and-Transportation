package com.transportlogistics.app.tracking.domain.speed;

import java.math.BigDecimal;
import java.util.Objects;

public record SpeedKph(BigDecimal value) implements Comparable<SpeedKph> {
    private static final BigDecimal MAXIMUM = new BigDecimal("400");

    public SpeedKph {
        Objects.requireNonNull(value, "Speed is required");
        if (value.signum() < 0 || value.compareTo(MAXIMUM) > 0) {
            throw new SpeedMonitoringException("SPEED_VALUE_INVALID", "Speed must be between 0 and 400 km/h");
        }
        value = value.stripTrailingZeros();
    }

    public static SpeedKph threshold(BigDecimal value) {
        SpeedKph speed = new SpeedKph(value);
        if (speed.value.signum() == 0) {
            throw new SpeedMonitoringException("SPEED_THRESHOLD_INVALID", "Threshold must be greater than zero");
        }
        return speed;
    }

    @Override
    public int compareTo(SpeedKph other) {
        return value.compareTo(other.value);
    }

    public boolean exceeds(SpeedKph threshold) {
        return compareTo(threshold) > 0;
    }
}
