package com.transportlogistics.app.tracking.ports.inbound;

import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeofenceQuery {
    Optional<Geofence> get(UUID tenantId, UUID geofenceId);

    Page<Geofence> list(UUID tenantId, GeofenceType type, GeofenceLifecycle lifecycle,
                        UUID locationId, int page, int size);

    Page<VehicleGeofenceState> memberships(UUID tenantId, UUID vehicleId,
                                           UUID geofenceId, int page, int size);

    CursorPage<GeofenceTransition> transitions(UUID tenantId, UUID geofenceId,
                                               UUID vehicleId, GeofenceType type,
                                               Instant from, Instant to,
                                               String cursor, int limit);

    CursorPage<GeofenceTransition> unauthorizedTransitions(UUID tenantId,
                                                           Instant from, Instant to,
                                                           String cursor, int limit);

    record Page<T>(List<T> items, int page, int size, long total) {
        public Page {
            items = List.copyOf(items);
        }
    }

    record CursorPage<T>(List<T> items, String nextCursor) {
        public CursorPage {
            items = List.copyOf(items);
        }
    }
}
