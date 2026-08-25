package com.transportlogistics.app.routing.domain.model;

import com.transportlogistics.app.routing.RouteTripMetric;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePerformanceCalculatorTest {

    @Test
    void calculatesZeroMetricsForEmptyTripList() {
        var routeId = UUID.randomUUID();
        var route = new Route(routeId, "RTE-01", "Test Route", UUID.randomUUID(), UUID.randomUUID(), 100.0, 120, true, List.of());

        var result = RoutePerformanceCalculator.calculate(route, List.of());

        assertThat(result.totalTripCount()).isEqualTo(0);
        assertThat(result.completedTripCount()).isEqualTo(0);
        assertThat(result.plannedDistanceKm()).isEqualTo(100.0);
        assertThat(result.averageActualDistanceKm()).isNull();
        assertThat(result.distanceVarianceKm()).isNull();
        assertThat(result.plannedDurationMinutes()).isEqualTo(120);
        assertThat(result.averageActualDurationMinutes()).isNull();
        assertThat(result.durationVarianceMinutes()).isNull();
        assertThat(result.onTimeTripCount()).isEqualTo(0);
        assertThat(result.delayedTripCount()).isEqualTo(0);
        assertThat(result.averageDelayMinutes()).isNull();
    }

    @Test
    void calculatesPlannedVsActualMetricsAndVariancesForCompletedTrips() {
        var routeId = UUID.randomUUID();
        var route = new Route(routeId, "RTE-01", "Colombo - Kandy", UUID.randomUUID(), UUID.randomUUID(), 100.0, 120, true, List.of());

        var now = OffsetDateTime.parse("2026-08-24T10:00:00Z");

        // Trip 1: 110 km (1000 to 1110), 150 mins (10:00 to 12:30), 20 mins reported delay -> Delayed
        var trip1 = new RouteTripMetric(
                UUID.randomUUID(),
                "TRIP-001",
                "COMPLETED",
                now,
                now.plusMinutes(120),
                now,
                now.plusMinutes(150),
                1000.0,
                1110.0,
                20
        );

        // Trip 2: 90 km (2000 to 2090), 110 mins (10:00 to 11:50), 0 delay -> On time
        var trip2 = new RouteTripMetric(
                UUID.randomUUID(),
                "TRIP-002",
                "COMPLETED",
                now,
                now.plusMinutes(120),
                now,
                now.plusMinutes(110),
                2000.0,
                2090.0,
                0
        );

        // Trip 3: Draft (not completed) -> Ignored in actuals
        var trip3 = new RouteTripMetric(
                UUID.randomUUID(),
                "TRIP-003",
                "DRAFT",
                now,
                now.plusMinutes(120),
                null,
                null,
                null,
                null,
                0
        );

        var result = RoutePerformanceCalculator.calculate(route, List.of(trip1, trip2, trip3));

        assertThat(result.totalTripCount()).isEqualTo(3);
        assertThat(result.completedTripCount()).isEqualTo(2);
        // Average actual distance: (110 + 90) / 2 = 100.0 km
        assertThat(result.averageActualDistanceKm()).isEqualTo(100.0);
        assertThat(result.distanceVarianceKm()).isEqualTo(0.0);
        assertThat(result.distanceVariancePercent()).isEqualTo(0.0);

        // Average actual duration: (150 + 110) / 2 = 130 mins
        assertThat(result.averageActualDurationMinutes()).isEqualTo(130);
        assertThat(result.durationVarianceMinutes()).isEqualTo(10); // 130 - 120
        assertThat(result.durationVariancePercent()).isEqualTo(8.33); // (10 / 120) * 100 = 8.333%

        // On-time vs delayed
        assertThat(result.onTimeTripCount()).isEqualTo(1);
        assertThat(result.delayedTripCount()).isEqualTo(1);
        assertThat(result.averageDelayMinutes()).isEqualTo(20.0);
    }
}
