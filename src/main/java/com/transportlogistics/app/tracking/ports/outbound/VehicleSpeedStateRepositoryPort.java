package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleSpeedStateRepositoryPort {
    Optional<VehicleSpeedState> findForUpdate(UUID tenantId, UUID vehicleId);
    default Optional<VehicleSpeedState> findState(UUID tenantId, UUID vehicleId) {
        return findForUpdate(tenantId, vehicleId);
    }
    VehicleSpeedState save(VehicleSpeedState state, long expectedVersion);
    List<VehicleSpeedState> find(UUID tenantId, VehicleSpeedState.MonitoringState state,
                                 int page, int size);
    long count(UUID tenantId, VehicleSpeedState.MonitoringState state);
}
