package com.transportlogistics.app.routing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RouteTest {
    @Test
    void acceptsAnOrderedMultiStopRoute() {
        var firstStop = UUID.randomUUID();
        var secondStop = UUID.randomUUID();

        var route = route(UUID.randomUUID(), UUID.randomUUID(), 125.5, 180, List.of(firstStop, secondStop));

        assertEquals(List.of(firstStop, secondStop), route.stopLocationIds());
        assertThrows(UnsupportedOperationException.class, () -> route.stopLocationIds().add(UUID.randomUUID()));
    }

    @Test
    void rejectsInvalidEndpointsDistanceAndDuration() {
        var location = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> route(location, location, 10.0, 20, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> route(UUID.randomUUID(), UUID.randomUUID(), 0.0, 20, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> route(UUID.randomUUID(), UUID.randomUUID(), 10.0, 0, List.of()));
    }

    @Test
    void rejectsDuplicateStopsAndEndpointStops() {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stop = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> route(origin, destination, 10.0, 20,
                List.of(stop, stop)));
        assertThrows(IllegalArgumentException.class, () -> route(origin, destination, 10.0, 20,
                List.of(origin)));
    }

    private Route route(UUID origin, UUID destination, Double distance, Integer duration, List<UUID> stops) {
        return new Route(UUID.randomUUID(), "RT-1", "Main route", origin, destination, distance, duration, true,
                stops);
    }
}
