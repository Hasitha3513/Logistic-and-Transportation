package com.transportlogistics.app.routing.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.domain.model.Route;
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
class RoutePerformanceIntegrationTest {

    @Autowired RouteUseCase routeUseCase;
    @Autowired RouteRepository routeRepo;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;

    @Test
    void aggregatesTripPerformanceMetricsFromDatabase() {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stop1 = UUID.randomUUID();
        locations(jdbc, origin, destination, stop1);

        var route = new Route(UUID.randomUUID(), "RT-PERF-" + suffix(), "Performance Route",
                origin, destination, 100.0, 120, true, List.of(stop1));
        routeRepo.save(route);
        em.flush();

        var now = OffsetDateTime.now();
        var tripId1 = UUID.randomUUID();
        var tripId2 = UUID.randomUUID();

        // Insert completed trip 1: 110 km actual, 150 mins actual, 20 mins delay
        insertTrip(tripId1, "TRP-P1-" + suffix(), route.id(), origin, destination, "COMPLETED",
                now.minusDays(2), now.minusDays(2).plusMinutes(120),
                now.minusDays(2), now.minusDays(2).plusMinutes(150),
                1000.0, 1110.0);
        insertDelayEvent(UUID.randomUUID(), tripId1, now.minusDays(2).plusMinutes(30), 20);

        // Insert completed trip 2: 90 km actual, 110 mins actual, 0 delay
        insertTrip(tripId2, "TRP-P2-" + suffix(), route.id(), origin, destination, "COMPLETED",
                now.minusDays(1), now.minusDays(1).plusMinutes(120),
                now.minusDays(1), now.minusDays(1).plusMinutes(110),
                2000.0, 2090.0);

        var performance = routeUseCase.getRoutePerformance(route.id(), now.minusDays(5), now.plusDays(1));

        assertNotNull(performance);
        assertEquals(route.id(), performance.routeId());
        assertEquals(2, performance.totalTripCount());
        assertEquals(2, performance.completedTripCount());
        assertEquals(100.0, performance.plannedDistanceKm());
        assertEquals(100.0, performance.averageActualDistanceKm());
        assertEquals(0.0, performance.distanceVarianceKm());
        assertEquals(130, performance.averageActualDurationMinutes());
        assertEquals(10, performance.durationVarianceMinutes());
        assertEquals(1, performance.onTimeTripCount());
        assertEquals(1, performance.delayedTripCount());
        assertEquals(20.0, performance.averageDelayMinutes());
    }

    @Test
    void filtersByRouteAndInclusiveDateBoundaries() {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        locations(jdbc, origin, destination);
        var route = new Route(UUID.randomUUID(), "RT-FILTER-" + suffix(), "Filtered Route",
                origin, destination, 80.0, 90, true, List.of());
        var otherRoute = new Route(UUID.randomUUID(), "RT-OTHER-" + suffix(), "Other Route",
                origin, destination, 80.0, 90, true, List.of());
        routeRepo.save(route);
        routeRepo.save(otherRoute);
        em.flush();

        var from = OffsetDateTime.now().minusDays(7).withNano(0);
        var to = from.plusDays(2);
        insertTrip(UUID.randomUUID(), "TRP-START-" + suffix(), route.id(), origin, destination, "COMPLETED",
                from, from.plusMinutes(90), from, from.plusMinutes(90), 100.0, 180.0);
        insertTrip(UUID.randomUUID(), "TRP-END-" + suffix(), route.id(), origin, destination, "CLOSED",
                to.minusMinutes(90), to, to.minusMinutes(90), to, 200.0, 280.0);
        insertTrip(UUID.randomUUID(), "TRP-OLD-" + suffix(), route.id(), origin, destination, "COMPLETED",
                from.minusSeconds(1), from.plusMinutes(89), from.minusSeconds(1), from.plusMinutes(89), 300.0, 380.0);
        insertTrip(UUID.randomUUID(), "TRP-OTHER-" + suffix(), otherRoute.id(), origin, destination, "COMPLETED",
                from.plusHours(1), from.plusMinutes(91), from.plusHours(1), from.plusMinutes(91), 400.0, 480.0);

        var performance = routeUseCase.getRoutePerformance(route.id(), from, to);

        assertEquals(2, performance.totalTripCount());
        assertEquals(2, performance.completedTripCount());
        assertEquals(80.0, performance.averageActualDistanceKm());
    }

    private void insertTrip(UUID id, String tripNumber, UUID routeId, UUID origin, UUID dest, String status,
                            OffsetDateTime reqStart, OffsetDateTime reqEnd,
                            OffsetDateTime actStart, OffsetDateTime actEnd,
                            Double startOdo, Double endOdo) {
        jdbc.update("""
                insert into trip (id, trip_number, priority, route_id, origin_location_id, destination_location_id,
                                 status, requested_start_time, requested_end_time, actual_start_time,
                                 actual_end_time, start_odometer_km, end_odometer_km, created_at, updated_at)
                values (?, ?, 'NORMAL', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, tripNumber, routeId, origin, dest, status, reqStart, reqEnd, actStart, actEnd, startOdo, endOdo, reqStart, reqStart);
    }

    private void insertDelayEvent(UUID id, UUID tripId, OffsetDateTime occurredAt, int delayMinutes) {
        jdbc.update("""
                insert into trip_operational_event (id, trip_id, event_type, occurred_at, delay_minutes,
                                                   recorded_by, created_at, updated_at)
                values (?, ?, 'DELAY', ?, ?, 'driver', ?, ?)
                """, id, tripId, occurredAt, delayMinutes, occurredAt, occurredAt);
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
