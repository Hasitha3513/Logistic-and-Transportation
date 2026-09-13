package com.transportlogistics.app.routing.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.domain.model.Route;
import com.transportlogistics.app.routing.domain.model.RouteRevision;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteAssignmentLookupProviderTest {
    @Test
    void publishesTheCurrentAuthoritativeRevision() {
        var routes = mock(RouteUseCase.class);
        var routeId = UUID.randomUUID();
        var originId = UUID.randomUUID();
        var destinationId = UUID.randomUUID();
        var route = new Route(routeId, "R-1", "Route", originId, destinationId, 12d, 30, true, List.of());
        var revision = new RouteRevision(UUID.randomUUID(), routeId, 8, "R-1", "Route", originId,
                destinationId, 12d, 30, true, List.of(), OffsetDateTime.parse("2026-09-01T00:00:00Z"),
                "planner");
        when(routes.get(routeId)).thenReturn(route);
        when(routes.getRevisions(routeId)).thenReturn(List.of(revision));

        var published = new RouteConfig().routeAssignmentLookup(routes).get(routeId);

        assertEquals(routeId, published.id());
        assertEquals("REVISION:8", published.routeVersion());
    }
}
