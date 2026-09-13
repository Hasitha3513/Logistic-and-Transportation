package com.transportlogistics.app.tracking.domain.routedeviation;

import java.math.BigDecimal;
import java.util.Objects;

public record RoutePoint(BigDecimal longitude, BigDecimal latitude) {
    public RoutePoint {
        Objects.requireNonNull(longitude, "Longitude is required");
        Objects.requireNonNull(latitude, "Latitude is required");
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new RouteDeviationException("INVALID_GEOMETRY", "Invalid WGS84 coordinate");
        }
    }
}
