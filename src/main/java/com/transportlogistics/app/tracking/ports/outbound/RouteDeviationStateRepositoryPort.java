package com.transportlogistics.app.tracking.ports.outbound;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;import java.util.*;
public interface RouteDeviationStateRepositoryPort {
    Optional<VehicleRouteDeviationState> find(UUID tenantId, UUID vehicleId);
    default List<VehicleRouteDeviationState> list(UUID tenantId,
            VehicleRouteDeviationState.State state, int offset, int size) {
        return List.of();
    }
    default long count(UUID tenantId, VehicleRouteDeviationState.State state) {
        return 0;
    }
    Optional<VehicleRouteDeviationState> lockAndFind(UUID tenantId, UUID vehicleId);
    VehicleRouteDeviationState save(VehicleRouteDeviationState state);
}
