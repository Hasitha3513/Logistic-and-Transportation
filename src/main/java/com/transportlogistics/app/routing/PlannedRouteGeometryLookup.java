package com.transportlogistics.app.routing;

import java.util.Optional;
import java.util.UUID;

/** Published Tenant-explicit read boundary for immutable route-revision geometry. */
public interface PlannedRouteGeometryLookup {
    Optional<PlannedRouteGeometry> find(UUID tenantId, UUID routeId, String routeVersion);
}
