package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.geofence.GeofencePosition;
import java.util.Optional;
import java.util.UUID;

public interface GeofencePositionRepositoryPort {
    Optional<GeofencePosition> find(UUID tenantId, UUID positionId);
}
