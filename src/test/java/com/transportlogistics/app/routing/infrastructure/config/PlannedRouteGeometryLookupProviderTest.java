package com.transportlogistics.app.routing.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transportlogistics.app.routing.PlannedRouteGeometryLookup;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlannedRouteGeometryLookupProviderTest {
    private final PlannedRouteGeometryLookup lookup = new RouteConfig().plannedRouteGeometryLookup();

    @Test
    void unavailableImmutableGeometryReturnsEmptyForEveryExactRevisionWithoutFallback() {
        UUID tenantId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        assertTrue(lookup.find(tenantId, routeId, "REVISION:1").isEmpty());
        assertTrue(lookup.find(tenantId, routeId, "REVISION:99").isEmpty());
    }

    @Test
    void requiresExplicitTenantRouteAndCanonicalRevision() {
        UUID tenantId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        assertThrows(NullPointerException.class,
                () -> lookup.find(null, routeId, "REVISION:1"));
        assertThrows(NullPointerException.class,
                () -> lookup.find(tenantId, null, "REVISION:1"));
        assertThrows(IllegalArgumentException.class,
                () -> lookup.find(tenantId, routeId, "LATEST"));
    }
}
