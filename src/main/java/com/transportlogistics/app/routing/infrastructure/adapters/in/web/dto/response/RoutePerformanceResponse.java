package com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response;

import java.util.UUID;

public record RoutePerformanceResponse(
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
}
