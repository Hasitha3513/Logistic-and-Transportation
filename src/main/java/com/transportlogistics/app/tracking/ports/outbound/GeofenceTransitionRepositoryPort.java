package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeofenceTransitionRepositoryPort {
    GeofenceTransition append(GeofenceTransition transition);

    Optional<GeofenceTransition> findByIdentity(UUID tenantId, UUID transitionId);

    List<GeofenceTransition> find(UUID tenantId, UUID geofenceId, UUID vehicleId,
                                  GeofenceType type, Instant from, Instant to,
                                  String cursor, int limit, boolean unauthorizedOnly);
}
