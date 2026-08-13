package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.domain.model.Route;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class RouteRepositoryIntegrationTest {
    @Autowired RouteRepository routes;

    @Test
    void persistsOrderedStopsAndFiltersRoutes() {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stops = List.of(UUID.randomUUID(), UUID.randomUUID());
        var active = route("RT-CENTRAL-" + suffix(), "Central distribution", origin, destination, true,
                stops);
        var inactive = route("RT-OLD-" + suffix(), "Retired corridor", UUID.randomUUID(), destination,
                false, List.of());
        routes.save(active);
        routes.save(inactive);

        assertEquals(stops, routes.findById(active.id()).orElseThrow().stopLocationIds());
        assertEquals(List.of(active), routes.search("central", origin, destination, true));
        assertEquals(List.of(inactive), routes.search("retired", null, null, false));
        assertTrue(routes.search(null, origin, null, false).isEmpty());
    }

    private Route route(String code, String name, UUID origin, UUID destination, boolean active, List<UUID> stops) {
        return new Route(UUID.randomUUID(), code, name, origin, destination, 100.0, 120, active, stops);
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
