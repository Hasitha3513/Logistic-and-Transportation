package com.transportlogistics.app.routing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteOptimizerTest {

    @Test
    void optimizationWithZeroOrOneStopReturnsOriginalSequenceWithoutChanges() {
        var routeId = UUID.randomUUID();
        var origin = UUID.randomUUID();
        var dest = UUID.randomUUID();

        // 0 stops
        var result0 = RouteOptimizer.optimize(routeId, origin, dest, List.of(), 100.0, 120, (a, b) -> 10.0);
        assertThat(result0.optimizedStopLocationIds()).isEmpty();
        assertThat(result0.distanceSavedKm()).isEqualTo(0.0);
        assertThat(result0.durationSavedMinutes()).isEqualTo(0);

        // 1 stop
        var stop1 = UUID.randomUUID();
        var result1 = RouteOptimizer.optimize(routeId, origin, dest, List.of(stop1), 100.0, 120, (a, b) -> 10.0);
        assertThat(result1.optimizedStopLocationIds()).containsExactly(stop1);
        assertThat(result1.distanceSavedKm()).isEqualTo(0.0);
    }

    @Test
    void optimizationReordersSuboptimalStopsToReduceTotalDistance() {
        var routeId = UUID.randomUUID();
        // Points on a 1D line: Origin(0), S1(30), S2(10), S3(20), Dest(40)
        // Original order: S1(30) -> S2(10) -> S3(20)
        // Original tour: 0 -> 30 -> 10 -> 20 -> 40
        //   d(0,30)=30, d(30,10)=20, d(10,20)=10, d(20,40)=20 => total = 80
        // Optimal tour: 0 -> S2(10) -> S3(20) -> S1(30) -> 40
        //   d(0,10)=10, d(10,20)=10, d(20,30)=10, d(30,40)=10 => total = 40

        var origin = UUID.randomUUID();
        var s1 = UUID.randomUUID();
        var s2 = UUID.randomUUID();
        var s3 = UUID.randomUUID();
        var dest = UUID.randomUUID();

        Map<UUID, Double> positions = Map.of(
                origin, 0.0,
                s1, 30.0,
                s2, 10.0,
                s3, 20.0,
                dest, 40.0
        );

        BiFunction<UUID, UUID, Double> distFn = (a, b) -> Math.abs(positions.get(a) - positions.get(b));

        var result = RouteOptimizer.optimize(routeId, origin, dest, List.of(s1, s2, s3), 80.0, 120, distFn);

        assertThat(result.optimizedStopLocationIds()).containsExactly(s2, s3, s1);
        assertThat(result.optimizedEstimatedDistanceKm()).isEqualTo(40.0);
        assertThat(result.distanceSavedKm()).isEqualTo(40.0);
        assertThat(result.optimizedEstimatedDurationMinutes()).isEqualTo(60);
        assertThat(result.durationSavedMinutes()).isEqualTo(60);
        assertThat(result.percentageDistanceImprovement()).isEqualTo(50.0);
    }

    @Test
    void optimizationIsStrictlyDeterministicAcrossMultipleRuns() {
        var routeId = UUID.randomUUID();
        var origin = UUID.randomUUID();
        var dest = UUID.randomUUID();

        List<UUID> stops = new ArrayList<>();
        Map<UUID, Double> coords = new HashMap<>();
        coords.put(origin, 0.0);
        coords.put(dest, 100.0);

        for (int i = 0; i < 15; i++) {
            var s = UUID.randomUUID();
            stops.add(s);
            coords.put(s, (i * 37) % 95 + 2.5);
        }

        BiFunction<UUID, UUID, Double> distFn = (a, b) -> Math.abs(coords.get(a) - coords.get(b));

        var firstRun = RouteOptimizer.optimize(routeId, origin, dest, stops, 500.0, 300, distFn);
        for (int i = 0; i < 5; i++) {
            var subsequentRun = RouteOptimizer.optimize(routeId, origin, dest, stops, 500.0, 300, distFn);
            assertThat(subsequentRun.optimizedStopLocationIds()).isEqualTo(firstRun.optimizedStopLocationIds());
            assertThat(subsequentRun.optimizedEstimatedDistanceKm()).isEqualTo(firstRun.optimizedEstimatedDistanceKm());
        }
    }

    @Test
    void optimizationHandlesMaximumAllowedStopsWithinBoundedExecutionTime() {
        var routeId = UUID.randomUUID();
        var origin = UUID.randomUUID();
        var dest = UUID.randomUUID();

        List<UUID> fiftyStops = new ArrayList<>();
        Map<UUID, Double> coords = new HashMap<>();
        coords.put(origin, 0.0);
        coords.put(dest, 1000.0);

        for (int i = 0; i < 50; i++) {
            var s = UUID.randomUUID();
            fiftyStops.add(s);
            coords.put(s, (double) ((i * 73) % 990 + 5));
        }

        BiFunction<UUID, UUID, Double> distFn = (a, b) -> Math.abs(coords.get(a) - coords.get(b));

        long start = System.currentTimeMillis();
        var result = RouteOptimizer.optimize(routeId, origin, dest, fiftyStops, 2000.0, 1500, distFn);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.optimizedStopLocationIds()).hasSize(50);
        assertThat(result.optimizedEstimatedDistanceKm()).isLessThanOrEqualTo(2000.0);
        assertThat(elapsed).isLessThan(500); // Must complete well within 500ms
    }

    @Test
    void optimizationRejectsExceedingFiftyStops() {
        var routeId = UUID.randomUUID();
        var origin = UUID.randomUUID();
        var dest = UUID.randomUUID();

        List<UUID> fiftyOneStops = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            fiftyOneStops.add(UUID.randomUUID());
        }

        assertThatThrownBy(() -> RouteOptimizer.optimize(routeId, origin, dest, fiftyOneStops, 100.0, 60, (a, b) -> 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot optimize routes with more than 50 stops");
    }

    @Test
    void optimizationHandlesTwoStopsAndNeverReturnsAWorseSequence() {
        var origin = UUID.randomUUID();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var positions = Map.of(origin, 0.0, first, 10.0, second, 20.0, destination, 30.0);

        var result = RouteOptimizer.optimize(UUID.randomUUID(), origin, destination, List.of(first, second),
                30.0, 30, (a, b) -> Math.abs(positions.get(a) - positions.get(b)));

        assertThat(result.optimizedStopLocationIds()).containsExactly(first, second);
        assertThat(result.optimizedEstimatedDistanceKm()).isLessThanOrEqualTo(result.originalEstimatedDistanceKm());
    }

    @Test
    void optimizationRejectsDuplicateStopsAndInvalidDistanceValues() {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var stop = UUID.randomUUID();

        assertThatThrownBy(() -> RouteOptimizer.optimize(UUID.randomUUID(), origin, destination,
                List.of(stop, stop), 10.0, 10, (a, b) -> 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unique");

        assertThatThrownBy(() -> RouteOptimizer.optimize(UUID.randomUUID(), origin, destination,
                List.of(stop, UUID.randomUUID()), 10.0, 10, (a, b) -> Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }
}
