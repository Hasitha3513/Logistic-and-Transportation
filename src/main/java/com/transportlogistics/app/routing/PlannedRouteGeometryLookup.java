package com.transportlogistics.app.routing;

import java.util.Optional;
import java.util.UUID;

/** Published Tenant-explicit read boundary for immutable route-revision geometry. */
public interface PlannedRouteGeometryLookup {
    /**
     * Finds the complete immutable geometry for exactly the requested Routing revision.
     * Implementations return empty rather than substituting a latest revision or endpoint chord.
     */
    Optional<PlannedRouteGeometry> find(UUID tenantId, UUID routeId, String routeVersion);
}
