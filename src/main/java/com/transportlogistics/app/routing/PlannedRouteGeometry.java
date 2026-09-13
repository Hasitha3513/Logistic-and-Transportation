package com.transportlogistics.app.routing;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable published snapshot of one Routing-owned route revision. */
public record PlannedRouteGeometry(UUID routeId, String routeVersion, List<Wgs84Point> orderedPoints) {
    public PlannedRouteGeometry {
        Objects.requireNonNull(routeId, "Route ID is required");
        routeVersion = requireVersion(routeVersion);
        orderedPoints = List.copyOf(Objects.requireNonNull(orderedPoints, "Ordered points are required"));
        if (orderedPoints.size() < 2 || orderedPoints.size() > 2_000) {
            throw new IllegalArgumentException("Route geometry requires 2 to 2000 points");
        }
        for (int i = 1; i < orderedPoints.size(); i++) {
            if (orderedPoints.get(i - 1).equals(orderedPoints.get(i))) {
                throw new IllegalArgumentException("Adjacent route points must be distinct");
            }
        }
    }

    private static String requireVersion(String value) {
        if (value == null || value.length() > 120 || !value.matches("REVISION:[1-9][0-9]*")) {
            throw new IllegalArgumentException("Route version must use REVISION:<positive-integer>");
        }
        return value;
    }
}
