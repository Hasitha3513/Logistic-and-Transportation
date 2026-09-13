package com.transportlogistics.app.routing.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transportlogistics.app.routing.PlannedRouteGeometryLookup;
import com.transportlogistics.app.routing.PlannedRouteGeometry;
import com.transportlogistics.app.routing.application.ports.out.RouteRevisionGeometryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlannedRouteGeometryLookupProviderTest {
    private final RouteRevisionGeometryRepository repository = new RouteRevisionGeometryRepository() {
        @Override
        public PlannedRouteGeometry save(
                UUID tenantId, UUID routeRevisionId, PlannedRouteGeometry geometry) {
            throw new UnsupportedOperationException("Saving is outside this provider test");
        }

        @Override
        public Optional<PlannedRouteGeometry> find(
                UUID tenantId, UUID routeId, String routeVersion) {
            return Optional.empty();
        }
    };
    private final PlannedRouteGeometryLookup lookup =
            new RouteConfig().plannedRouteGeometryLookup(repository);

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
