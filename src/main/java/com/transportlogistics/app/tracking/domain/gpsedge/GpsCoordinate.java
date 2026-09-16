package com.transportlogistics.app.tracking.domain.gpsedge;

import java.math.BigDecimal;
import java.util.Objects;

public record GpsCoordinate(BigDecimal latitude, BigDecimal longitude) {
    private static final int MAXIMUM_FRACTIONAL_DIGITS = 7;

    public GpsCoordinate {
        Objects.requireNonNull(latitude, "latitude is required");
        Objects.requireNonNull(longitude, "longitude is required");
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new GpsEdgeCaseException("INVALID_COORDINATE", "Coordinate is outside WGS84 bounds");
        }
        if (normalizedScale(latitude) > MAXIMUM_FRACTIONAL_DIGITS
                || normalizedScale(longitude) > MAXIMUM_FRACTIONAL_DIGITS) {
            throw new GpsEdgeCaseException(
                    "INVALID_COORDINATE_PRECISION", "Coordinate exceeds seven fractional digits");
        }
    }

    public boolean isNullIsland() {
        return latitude.compareTo(BigDecimal.ZERO) == 0 && longitude.compareTo(BigDecimal.ZERO) == 0;
    }

    private static int normalizedScale(BigDecimal value) {
        return Math.max(0, value.stripTrailingZeros().scale());
    }
}
