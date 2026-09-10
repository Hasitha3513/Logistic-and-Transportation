package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import java.time.Instant;
import java.util.UUID;

public interface GeofenceManagementUseCase {
    Geofence create(Context context, CreateGeofence command);

    Geofence update(Context context, UUID geofenceId, long expectedVersion,
                    UpdateGeofence command);

    Geofence activate(Context context, UUID geofenceId, long expectedVersion);

    Geofence disable(Context context, UUID geofenceId, long expectedVersion, String reason);

    Geofence retire(Context context, UUID geofenceId, long expectedVersion, String reason);

    record Context(UUID tenantId, UUID actorId, String correlationId, Instant now) {
    }

    record CreateGeofence(String name, GeofenceType type, GeofencePolygon polygon,
                          UUID locationId, GeofenceAlertPolicy alertPolicy) {
    }

    record UpdateGeofence(String name, GeofenceType type, GeofencePolygon polygon,
                          UUID locationId, GeofenceAlertPolicy alertPolicy) {
    }
}
