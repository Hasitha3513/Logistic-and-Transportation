package com.transportlogistics.app.tracking.domain.geofence;

public record GeofenceAlertPolicy(boolean alertOnEntry, boolean alertOnExit) {
    public static GeofenceAlertPolicy unauthorizedZone() {
        return new GeofenceAlertPolicy(true, false);
    }
}
