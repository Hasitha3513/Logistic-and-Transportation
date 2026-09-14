package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.routedeviation.RoutePolyline;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import java.util.Optional;
import java.util.UUID;

public interface RouteDeviationGeometryLookupPort {
    Optional<RoutePolyline> find(UUID tenantId, UUID routeId, RouteVersion routeVersion);
}
