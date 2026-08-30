package com.transportlogistics.app.routing.domain.model;

import com.transportlogistics.app.routing.RouteTripMetric;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class RoutePerformanceCalculator {

    private RoutePerformanceCalculator() {
    }

    public static RoutePerformanceAnalytics calculate(
            Route route,
            List<RouteTripMetric> trips
    ) {
        Objects.requireNonNull(route, "route is required");
        List<RouteTripMetric> tripList = trips == null ? List.of() : trips;

        long totalTripCount = tripList.size();
        List<RouteTripMetric> completedTrips = tripList.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.status()) || "CLOSED".equalsIgnoreCase(t.status()))
                .toList();
        long completedTripCount = completedTrips.size();

        double plannedDistance = route.plannedDistanceKm();
        int plannedDuration = route.estimatedDurationMinutes();

        // 1. Distance metrics
        List<Double> actualDistances = completedTrips.stream()
                .filter(t -> t.startOdometerKm() != null && t.endOdometerKm() != null && t.endOdometerKm() >= t.startOdometerKm())
                .map(t -> t.endOdometerKm() - t.startOdometerKm())
                .toList();

        Double avgActualDistance = actualDistances.isEmpty()
                ? null
                : actualDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        Double distanceVarianceKm = avgActualDistance == null ? null : (avgActualDistance - plannedDistance);
        Double distanceVariancePercent = (distanceVarianceKm == null || plannedDistance <= 0)
                ? null
                : distanceVarianceKm / plannedDistance * 100.0;

        // 2. Duration metrics
        List<Long> actualDurations = completedTrips.stream()
                .filter(t -> t.actualStartTime() != null && t.actualEndTime() != null && !t.actualEndTime().isBefore(t.actualStartTime()))
                .map(t -> Duration.between(t.actualStartTime(), t.actualEndTime()).toMinutes())
                .toList();

        Integer avgActualDuration = actualDurations.isEmpty()
                ? null
                : (int) Math.round(actualDurations.stream().mapToLong(Long::longValue).average().orElse(0.0));

        Integer durationVarianceMinutes = avgActualDuration == null ? null : (avgActualDuration - plannedDuration);
        Double durationVariancePercent = (durationVarianceMinutes == null || plannedDuration <= 0)
                ? null
                : (double) durationVarianceMinutes / plannedDuration * 100.0;

        // 3. On-time vs Delayed counts
        long delayedCount = 0;
        long onTimeCount = 0;
        long totalDelayMins = 0;

        for (RouteTripMetric trip : completedTrips) {
            boolean isDelayed = false;
            int delay = trip.totalDelayMinutes();

            if (delay > 0) {
                isDelayed = true;
                totalDelayMins += delay;
            } else if (trip.actualStartTime() != null && trip.actualEndTime() != null) {
                long duration = Duration.between(trip.actualStartTime(), trip.actualEndTime()).toMinutes();
                if (duration > plannedDuration) {
                    isDelayed = true;
                    totalDelayMins += (duration - plannedDuration);
                }
            }

            if (isDelayed) {
                delayedCount++;
            } else {
                onTimeCount++;
            }
        }

        Double avgDelayMinutes = delayedCount > 0 ? (double) totalDelayMins / delayedCount : null;

        return new RoutePerformanceAnalytics(
                route.id(),
                route.code(),
                route.name(),
                totalTripCount,
                completedTripCount,
                plannedDistance,
                avgActualDistance,
                distanceVarianceKm,
                distanceVariancePercent,
                plannedDuration,
                avgActualDuration,
                durationVarianceMinutes,
                durationVariancePercent,
                onTimeCount,
                delayedCount,
                avgDelayMinutes
        );
    }
}
