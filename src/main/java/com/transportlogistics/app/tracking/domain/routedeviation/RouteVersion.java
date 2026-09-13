package com.transportlogistics.app.tracking.domain.routedeviation;

import java.math.BigInteger;

public record RouteVersion(String value) {
    public RouteVersion {
        if (value == null || value.length() > 120 || !value.matches("REVISION:[1-9][0-9]*")) {
            throw error("INVALID_ROUTE_VERSION", "Route version must use REVISION:<positive-integer>");
        }
    }

    public BigInteger revision() {
        return new BigInteger(value.substring("REVISION:".length()));
    }
    public static RouteVersion ofRevision(int revision) {
        if (revision < 1) throw error("INVALID_ROUTE_VERSION", "Route revision must be positive");
        return new RouteVersion("REVISION:" + revision);
    }
    private static RouteDeviationException error(String code, String message) {
        return new RouteDeviationException(code, message);
    }
}
