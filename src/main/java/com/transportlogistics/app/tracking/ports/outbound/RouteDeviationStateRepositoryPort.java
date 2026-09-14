package com.transportlogistics.app.tracking.ports.outbound;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;import java.util.*;
public interface RouteDeviationStateRepositoryPort {
    Optional<VehicleRouteDeviationState> find(UUID tenantId, UUID vehicleId);
    Optional<VehicleRouteDeviationState> lockAndFind(UUID tenantId, UUID vehicleId);
    VehicleRouteDeviationState save(VehicleRouteDeviationState state);
}
