package com.transportlogistics.app.routing.application.ports.out;

import com.transportlogistics.app.routing.PlannedRouteGeometry;
import java.util.Optional;
import java.util.UUID;

/** Routing-owned persistence boundary for immutable published revision geometry. */
public interface RouteRevisionGeometryRepository {
    PlannedRouteGeometry save(UUID tenantId, UUID routeRevisionId, PlannedRouteGeometry geometry);

    Optional<PlannedRouteGeometry> find(UUID tenantId, UUID routeId, String routeVersion);
}
