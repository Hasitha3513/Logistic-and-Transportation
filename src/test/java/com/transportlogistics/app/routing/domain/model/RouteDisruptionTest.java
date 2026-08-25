package com.transportlogistics.app.routing.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RouteDisruptionTest {

    @Test
    void createsActiveDisruptionWithValidFields() {
        var routeId = UUID.randomUUID();
        var detourId = UUID.randomUUID();
        var from = OffsetDateTime.now();
        var until = from.plusHours(4);

        var disruption = RouteDisruption.create(
                routeId,
                RouteDisruptionType.ROAD_CLOSURE,
                DisruptionSeverity.HIGH,
                "Bridge maintenance closure on sector 4",
                from,
                until,
                detourId,
                from,
                "traffic_ops"
        );

        assertNotNull(disruption.id());
        assertEquals(routeId, disruption.routeId());
        assertEquals(RouteDisruptionType.ROAD_CLOSURE, disruption.disruptionType());
        assertEquals(DisruptionSeverity.HIGH, disruption.severity());
        assertEquals("Bridge maintenance closure on sector 4", disruption.description());
        assertEquals(from, disruption.effectiveFrom());
        assertEquals(until, disruption.effectiveUntil());
        assertEquals(detourId, disruption.detourRouteId());
        assertEquals(DisruptionStatus.ACTIVE, disruption.status());
        assertNull(disruption.resolvedAt());
        assertNull(disruption.resolvedBy());
    }

    @Test
    void resolvesActiveDisruption() {
        var routeId = UUID.randomUUID();
        var from = OffsetDateTime.now();
        var disruption = RouteDisruption.create(
                routeId,
                RouteDisruptionType.ACCIDENT,
                DisruptionSeverity.MEDIUM,
                "Multi-vehicle accident cleared",
                from,
                null,
                null,
                from,
                "dispatcher"
        );

        var resolvedAt = from.plusHours(1);
        var resolved = disruption.resolve(resolvedAt, "supervisor");

        assertEquals(DisruptionStatus.RESOLVED, resolved.status());
        assertEquals(resolvedAt, resolved.resolvedAt());
        assertEquals("supervisor", resolved.resolvedBy());
    }

    @Test
    void rejectsResolvingAlreadyResolvedDisruption() {
        var routeId = UUID.randomUUID();
        var from = OffsetDateTime.now();
        var disruption = RouteDisruption.create(
                routeId,
                RouteDisruptionType.WEATHER,
                DisruptionSeverity.CRITICAL,
                "Flash flood warning",
                from,
                null,
                null,
                from,
                "dispatcher"
        );

        var resolved = disruption.resolve(from.plusHours(2), "supervisor");
        assertThrows(IllegalStateException.class, () -> resolved.resolve(from.plusHours(3), "supervisor"));
    }

    @Test
    void rejectsInvalidEffectiveTimeWindow() {
        var routeId = UUID.randomUUID();
        var from = OffsetDateTime.now();
        var until = from.minusHours(1);

        assertThrows(IllegalArgumentException.class, () ->
                RouteDisruption.create(routeId, RouteDisruptionType.ROAD_CLOSURE, DisruptionSeverity.HIGH,
                        "Desc", from, until, null, from, "user")
        );
    }

    @Test
    void rejectsDetourRouteSameAsDisruptedRoute() {
        var routeId = UUID.randomUUID();
        var from = OffsetDateTime.now();

        assertThrows(IllegalArgumentException.class, () ->
                RouteDisruption.create(routeId, RouteDisruptionType.ROAD_CLOSURE, DisruptionSeverity.HIGH,
                        "Desc", from, null, routeId, from, "user")
        );
    }
}
