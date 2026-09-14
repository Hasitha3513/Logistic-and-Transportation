package com.transportlogistics.app.tracking.adapters.outbound.routing;

import com.transportlogistics.app.routing.PlannedRouteGeometryLookup;
import com.transportlogistics.app.tracking.domain.routedeviation.RoutePoint;
import com.transportlogistics.app.tracking.domain.routedeviation.RoutePolyline;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationGeometryLookupPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RoutingRouteDeviationGeometryAdapter implements RouteDeviationGeometryLookupPort {
    private final PlannedRouteGeometryLookup geometries;

    RoutingRouteDeviationGeometryAdapter(PlannedRouteGeometryLookup geometries) {
        this.geometries = geometries;
    }

    @Override
    public Optional<RoutePolyline> find(UUID tenantId, UUID routeId, RouteVersion routeVersion) {
        return geometries.find(tenantId, routeId, routeVersion.value()).map(geometry ->
                new RoutePolyline(routeVersion, geometry.orderedPoints().stream()
                        .map(point -> new RoutePoint(point.longitude(), point.latitude())).toList()));
    }
}
