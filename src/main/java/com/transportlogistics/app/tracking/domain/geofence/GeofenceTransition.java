package com.transportlogistics.app.tracking.domain.geofence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GeofenceTransition(UUID transitionId, UUID tenantId, UUID geofenceId,
                                 UUID vehicleId, UUID locationId, GeofenceType geofenceType,
                                 GeofenceTransitionType transitionType, GeofenceSeverity severity,
                                 Instant sourceTimestamp, long definitionVersion,
                                 UUID confirmingPositionId, GeofenceMembership fromState,
                                 GeofenceMembership toState) {
    public GeofenceTransition {
        Objects.requireNonNull(transitionId, "Transition ID is required");
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(geofenceId, "Geofence ID is required");
        Objects.requireNonNull(vehicleId, "Vehicle ID is required");
        Objects.requireNonNull(geofenceType, "Geofence type is required");
        Objects.requireNonNull(transitionType, "Transition type is required");
        Objects.requireNonNull(severity, "Severity is required");
        Objects.requireNonNull(sourceTimestamp, "Source timestamp is required");
        Objects.requireNonNull(confirmingPositionId, "Confirming position is required");
        Objects.requireNonNull(fromState, "From state is required");
        Objects.requireNonNull(toState, "To state is required");
        if (definitionVersion < 0) {
            throw new IllegalArgumentException("Definition version cannot be negative");
        }
    }

    public boolean alertRequired(GeofenceAlertPolicy policy) {
        return transitionType == GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED
                || transitionType == GeofenceTransitionType.ENTERED && policy.alertOnEntry()
                || transitionType == GeofenceTransitionType.EXITED && policy.alertOnExit();
    }
}
