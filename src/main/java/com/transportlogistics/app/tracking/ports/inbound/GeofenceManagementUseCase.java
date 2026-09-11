package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import java.time.Instant;
import java.util.UUID;

public interface GeofenceManagementUseCase {
    Geofence create(Context context, CreateGeofence command, String idempotencyKey);

    Geofence update(Context context, UUID geofenceId, long expectedVersion,
                    UpdateGeofence command);

    Geofence activate(Context context, UUID geofenceId, long expectedVersion, String idempotencyKey);

    Geofence disable(Context context, UUID geofenceId, long expectedVersion, String reason,
                     String idempotencyKey);

    Geofence retire(Context context, UUID geofenceId, long expectedVersion, String reason,
                    String idempotencyKey);

    record Context(UUID tenantId, UUID actorId, String correlationId, Instant now) {
    }

    record CreateGeofence(String name, GeofenceType type, GeofencePolygon polygon,
                          UUID locationId, GeofenceAlertPolicy alertPolicy) {
    }

    record UpdateGeofence(String name, GeofenceType type, GeofencePolygon polygon,
                          UUID locationId, GeofenceAlertPolicy alertPolicy) {
    }
}
