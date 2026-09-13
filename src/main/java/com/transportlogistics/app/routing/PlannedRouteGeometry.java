package com.transportlogistics.app.routing;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable published snapshot of one Routing-owned route revision. */
public record PlannedRouteGeometry(UUID routeId, String routeVersion, List<Wgs84Point> orderedPoints) {
    private static final double EARTH_RADIUS_METERS = 6_371_000d;
    private static final double MAX_SEGMENT_METERS = 25_000d;

    public PlannedRouteGeometry {
        Objects.requireNonNull(routeId, "Route ID is required");
        routeVersion = requireVersion(routeVersion);
        orderedPoints = List.copyOf(Objects.requireNonNull(orderedPoints, "Ordered points are required"));
        if (orderedPoints.size() < 2 || orderedPoints.size() > 2_000) {
            throw new IllegalArgumentException("Route geometry requires 2 to 2000 points");
        }
        for (int i = 1; i < orderedPoints.size(); i++) {
            Wgs84Point previous = orderedPoints.get(i - 1);
            Wgs84Point current = orderedPoints.get(i);
            if (previous.equals(current)) {
                throw new IllegalArgumentException("Adjacent route points must be distinct");
            }
            if (previous.longitude().subtract(current.longitude()).abs()
                    .compareTo(java.math.BigDecimal.valueOf(180)) > 0) {
                throw new IllegalArgumentException("Antimeridian-crossing route geometry is unsupported");
            }
            if (greatCircleMeters(previous, current) > MAX_SEGMENT_METERS + 0.001d) {
                throw new IllegalArgumentException("Route geometry segments cannot exceed 25 km");
            }
        }
    }

    private static double greatCircleMeters(Wgs84Point first, Wgs84Point second) {
        double firstLatitude = Math.toRadians(first.latitude().doubleValue());
        double secondLatitude = Math.toRadians(second.latitude().doubleValue());
        double latitudeDelta = secondLatitude - firstLatitude;
        double longitudeDelta = Math.toRadians(
                second.longitude().subtract(first.longitude()).doubleValue());
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 2 * EARTH_RADIUS_METERS
                * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private static String requireVersion(String value) {
        if (value == null || value.length() > 120 || !value.matches("REVISION:[1-9][0-9]*")) {
            throw new IllegalArgumentException("Route version must use REVISION:<positive-integer>");
        }
        return value;
    }
}
