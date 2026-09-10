package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeofenceRepositoryPort {
    Geofence save(Geofence geofence, long expectedVersion);

    Optional<Geofence> find(UUID tenantId, UUID geofenceId);

    List<Geofence> find(UUID tenantId, GeofenceType type, GeofenceLifecycle lifecycle,
                        UUID locationId, int page, int size);

    List<Geofence> findActiveCandidates(UUID tenantId, double longitude, double latitude,
                                        int limit);
}
