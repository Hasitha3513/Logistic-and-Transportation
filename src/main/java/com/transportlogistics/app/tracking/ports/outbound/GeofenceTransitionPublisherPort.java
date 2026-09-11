package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.geofence.GeofenceSeverity;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransitionType;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import java.time.Instant;
import java.util.UUID;

public interface GeofenceTransitionPublisherPort {
    void publish(VehicleGeofenceTransitionedV1 event);

    record VehicleGeofenceTransitionedV1(UUID eventId, UUID tenantId, UUID geofenceId,
                                         UUID vehicleId, UUID locationId,
                                         GeofenceType geofenceType,
                                         GeofenceTransitionType transition,
                                         GeofenceSeverity severity,
                                         Instant sourceTimestamp, long definitionVersion) {
    }
}
