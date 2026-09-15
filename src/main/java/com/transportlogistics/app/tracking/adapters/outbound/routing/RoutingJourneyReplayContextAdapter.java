package com.transportlogistics.app.tracking.adapters.outbound.routing;

import com.transportlogistics.app.routing.PlannedRouteGeometryLookup;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coordinate;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayRouteContextPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class RoutingJourneyReplayContextAdapter implements JourneyReplayRouteContextPort {
    private final PlannedRouteGeometryLookup geometries;

    RoutingJourneyReplayContextAdapter(PlannedRouteGeometryLookup geometries) {
        this.geometries = geometries;
    }

    @Override
    public Optional<List<Coordinate>> findExact(UUID tenantId, UUID routeId, String routeVersion) {
        return geometries.find(tenantId, routeId, routeVersion).map(geometry ->
                geometry.orderedPoints().stream()
                        .map(point -> new Coordinate(point.latitude(), point.longitude())).toList());
    }
}
