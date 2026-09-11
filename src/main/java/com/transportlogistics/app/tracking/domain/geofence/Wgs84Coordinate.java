package com.transportlogistics.app.tracking.domain.geofence;

public record Wgs84Coordinate(double longitude, double latitude) {
    public Wgs84Coordinate {
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw invalid("Longitude must be finite and between -180 and 180");
        }
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw invalid("Latitude must be finite and between -90 and 90");
        }
    }

    private static GeofenceRuleException invalid(String message) {
        return new GeofenceRuleException("GEOFENCE_INVALID_GEOMETRY", message);
    }
}
