package com.transportlogistics.app.routing.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RouteRevisionTest {

    @Test
    void createsValidRouteRevisionFromRoute() {
        var routeId = UUID.randomUUID();
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stop1 = UUID.randomUUID();
        var stop2 = UUID.randomUUID();
        var route = new Route(routeId, "RT-100", "North-South Expressway", origin, destination, 150.0, 120, true, List.of(stop1, stop2));

        var now = OffsetDateTime.now();
        var revision = RouteRevision.from(route, 1, now, "dispatcher1");

        assertNotNull(revision.id());
        assertEquals(routeId, revision.routeId());
        assertEquals(1, revision.revisionNumber());
        assertEquals("RT-100", revision.code());
        assertEquals("North-South Expressway", revision.name());
        assertEquals(origin, revision.originLocationId());
        assertEquals(destination, revision.destinationLocationId());
        assertEquals(150.0, revision.plannedDistanceKm());
        assertEquals(120, revision.estimatedDurationMinutes());
        assertTrue(revision.active());
        assertEquals(List.of(stop1, stop2), revision.stopLocationIds());
        assertEquals(now, revision.changedAt());
        assertEquals("dispatcher1", revision.changedBy());
    }

    @Test
    void rejectsInvalidRevisionNumber() {
        var routeId = UUID.randomUUID();
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var now = OffsetDateTime.now();

        assertThrows(IllegalArgumentException.class, () ->
                new RouteRevision(UUID.randomUUID(), routeId, 0, "RT-100", "Route", origin, destination, 100.0, 60, true, List.of(), now, "user")
        );
        assertThrows(IllegalArgumentException.class, () ->
                new RouteRevision(UUID.randomUUID(), routeId, -1, "RT-100", "Route", origin, destination, 100.0, 60, true, List.of(), now, "user")
        );
    }

    @Test
    void preservesStopOrderImmutably() {
        var routeId = UUID.randomUUID();
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stop1 = UUID.randomUUID();
        var stop2 = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var revision = new RouteRevision(UUID.randomUUID(), routeId, 2, "RT-100", "Route", origin, destination, 100.0, 60, true, List.of(stop1, stop2), now, "user");

        assertEquals(2, revision.stopLocationIds().size());
        assertEquals(stop1, revision.stopLocationIds().get(0));
        assertEquals(stop2, revision.stopLocationIds().get(1));
    }
}
