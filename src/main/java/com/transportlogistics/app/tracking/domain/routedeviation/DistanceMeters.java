package com.transportlogistics.app.tracking.domain.routedeviation;

import java.math.BigDecimal;
import java.util.Objects;

public record DistanceMeters(BigDecimal value) implements Comparable<DistanceMeters> {
    public DistanceMeters {
        Objects.requireNonNull(value, "Distance is required");
        if (value.signum() < 0) throw new RouteDeviationException("INVALID_DISTANCE", "Distance cannot be negative");
    }
    public static DistanceMeters of(double value) {
        if (!Double.isFinite(value) || value < 0) throw new RouteDeviationException("INVALID_DISTANCE", "Distance must be finite and nonnegative");
        return new DistanceMeters(BigDecimal.valueOf(value));
    }
    public DistanceMeters add(DistanceMeters other) { return new DistanceMeters(value.add(other.value)); }
    public DistanceMeters twice() { return new DistanceMeters(value.multiply(BigDecimal.valueOf(2))); }
    @Override public int compareTo(DistanceMeters other) { return value.compareTo(other.value); }
}
