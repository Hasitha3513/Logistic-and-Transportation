package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.application.ports.out.RouteDisruptionRepository;
import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.locations;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RouteDisruptionPersistenceIntegrationTest {
    @Autowired RouteRepository routes;
    @Autowired RouteDisruptionRepository disruptionRepo;
    @Autowired JdbcTemplate jdbc;

    @Test
    void persistsAndQueriesActiveDisruptions() {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        locations(jdbc, origin, destination);

        var route = new Route(UUID.randomUUID(), "RT-DIS-" + suffix(), "Disrupted Corridor",
                origin, destination, 80.0, 60, true, List.of());
        var detour = new Route(UUID.randomUUID(), "RT-DET-" + suffix(), "Detour Corridor",
                origin, destination, 95.0, 75, true, List.of());
        routes.save(route);
        routes.save(detour);

        var from = OffsetDateTime.now();
        var until = from.plusDays(1);
        var disruption = RouteDisruption.create(
                route.id(),
                RouteDisruptionType.ROAD_CLOSURE,
                DisruptionSeverity.CRITICAL,
                "Bridge structural repair",
                from,
                until,
                detour.id(),
                from,
                "traffic_authority"
        );
        disruptionRepo.save(disruption);

        var byRoute = disruptionRepo.findByRouteId(route.id());
        assertEquals(1, byRoute.size());
        assertEquals(RouteDisruptionType.ROAD_CLOSURE, byRoute.get(0).disruptionType());
        assertEquals(DisruptionSeverity.CRITICAL, byRoute.get(0).severity());
        assertEquals(detour.id(), byRoute.get(0).detourRouteId());
        assertEquals(DisruptionStatus.ACTIVE, byRoute.get(0).status());

        var activeList = disruptionRepo.findByStatus(DisruptionStatus.ACTIVE);
        assertTrue(activeList.stream().anyMatch(d -> d.id().equals(disruption.id())));

        // Resolve disruption
        var resolved = disruption.resolve(OffsetDateTime.now(), "supervisor");
        disruptionRepo.save(resolved);

        var updated = disruptionRepo.findById(disruption.id()).orElseThrow();
        assertEquals(DisruptionStatus.RESOLVED, updated.status());
        assertEquals("supervisor", updated.resolvedBy());
        assertNotNull(updated.resolvedAt());
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
