package com.transportlogistics.app.tracking.domain.geofence;

import java.util.Optional;

public record GeofenceEvaluationResult(VehicleGeofenceState state,
                                       Optional<GeofenceTransition> transition) {
    public GeofenceEvaluationResult {
        transition = transition == null ? Optional.empty() : transition;
    }

    public static GeofenceEvaluationResult withoutTransition(VehicleGeofenceState state) {
        return new GeofenceEvaluationResult(state, Optional.empty());
    }
}
