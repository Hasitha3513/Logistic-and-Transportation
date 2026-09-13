package com.transportlogistics.app.routing;

import java.math.BigDecimal;
import java.util.Objects;

/** Provider-neutral coordinate in canonical longitude/latitude order. */
public record Wgs84Point(BigDecimal longitude, BigDecimal latitude) {
    public Wgs84Point {
        Objects.requireNonNull(longitude, "Longitude is required");
        Objects.requireNonNull(latitude, "Latitude is required");
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
    }
}
