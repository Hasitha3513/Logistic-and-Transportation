package com.transportlogistics.app.routing.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RouteOptimizationResult(
        UUID routeId,
        List<UUID> originalStopLocationIds,
        List<UUID> optimizedStopLocationIds,
        double originalEstimatedDistanceKm,
        double optimizedEstimatedDistanceKm,
        int originalEstimatedDurationMinutes,
        int optimizedEstimatedDurationMinutes,
        double distanceSavedKm,
        int durationSavedMinutes,
        double percentageDistanceImprovement
) {
    public RouteOptimizationResult {
        Objects.requireNonNull(routeId, "routeId is required");
        originalStopLocationIds = originalStopLocationIds == null ? List.of() : List.copyOf(originalStopLocationIds);
        optimizedStopLocationIds = optimizedStopLocationIds == null ? List.of() : List.copyOf(optimizedStopLocationIds);
        originalEstimatedDistanceKm = round(originalEstimatedDistanceKm);
        optimizedEstimatedDistanceKm = round(optimizedEstimatedDistanceKm);
        distanceSavedKm = round(distanceSavedKm);
        percentageDistanceImprovement = round(percentageDistanceImprovement);
    }

    private static double round(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) {
            return 0.0;
        }
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
