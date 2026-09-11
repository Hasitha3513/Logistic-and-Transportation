package com.transportlogistics.app.tracking.domain.geofence;

public record BoundingBox(double minLongitude, double minLatitude,
                          double maxLongitude, double maxLatitude) {
    public boolean contains(Wgs84Coordinate point) {
        return point.longitude() >= minLongitude && point.longitude() <= maxLongitude
                && point.latitude() >= minLatitude && point.latitude() <= maxLatitude;
    }
}
