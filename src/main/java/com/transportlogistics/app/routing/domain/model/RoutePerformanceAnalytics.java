package com.transportlogistics.app.routing.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record RoutePerformanceAnalytics(
        UUID routeId,
        String routeCode,
        String routeName,
        long totalTripCount,
        long completedTripCount,
        double plannedDistanceKm,
        Double averageActualDistanceKm,
        Double distanceVarianceKm,
        Double distanceVariancePercent,
        int plannedDurationMinutes,
        Integer averageActualDurationMinutes,
        Integer durationVarianceMinutes,
        Double durationVariancePercent,
        long onTimeTripCount,
        long delayedTripCount,
        Double averageDelayMinutes
) {
    public RoutePerformanceAnalytics {
        Objects.requireNonNull(routeId, "routeId is required");
        plannedDistanceKm = round(plannedDistanceKm);
        if (averageActualDistanceKm != null) {
            averageActualDistanceKm = round(averageActualDistanceKm);
        }
        if (distanceVarianceKm != null) {
            distanceVarianceKm = round(distanceVarianceKm);
        }
        if (distanceVariancePercent != null) {
            distanceVariancePercent = round(distanceVariancePercent);
        }
        if (durationVariancePercent != null) {
            durationVariancePercent = round(durationVariancePercent);
        }
        if (averageDelayMinutes != null) {
            averageDelayMinutes = round(averageDelayMinutes);
        }
    }

    private static double round(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return 0.0;
        }
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
