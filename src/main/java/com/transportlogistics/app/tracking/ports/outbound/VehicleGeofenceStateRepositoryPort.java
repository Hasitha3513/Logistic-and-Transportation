package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleGeofenceStateRepositoryPort {
    Optional<VehicleGeofenceState> findForUpdate(UUID tenantId, UUID geofenceId, UUID vehicleId);

    VehicleGeofenceState save(VehicleGeofenceState state, long expectedVersion);

    List<VehicleGeofenceState> find(UUID tenantId, UUID vehicleId,
                                    UUID geofenceId, int page, int size);

    long count(UUID tenantId, UUID vehicleId, UUID geofenceId);
}
