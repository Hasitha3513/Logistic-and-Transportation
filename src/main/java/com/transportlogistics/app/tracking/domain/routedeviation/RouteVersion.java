package com.transportlogistics.app.tracking.domain.routedeviation;

public record RouteVersion(String value) {
    public RouteVersion {
        if (value == null || value.length() > 120 || !value.matches("REVISION:[1-9][0-9]*")) {
            throw error("INVALID_ROUTE_VERSION", "Route version must use REVISION:<positive-integer>");
        }
        try {
            Integer.parseInt(value.substring("REVISION:".length()));
        } catch (NumberFormatException exception) {
            throw error("INVALID_ROUTE_VERSION", "Route revision must fit a positive integer");
        }
    }

    public int revision() { return Integer.parseInt(value.substring("REVISION:".length())); }
    public static RouteVersion ofRevision(int revision) {
        if (revision < 1) throw error("INVALID_ROUTE_VERSION", "Route revision must be positive");
        return new RouteVersion("REVISION:" + revision);
    }
    private static RouteDeviationException error(String code, String message) {
        return new RouteDeviationException(code, message);
    }
}
