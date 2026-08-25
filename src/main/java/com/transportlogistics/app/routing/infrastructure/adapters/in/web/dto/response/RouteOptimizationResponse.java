package com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response;

import java.util.List;
import java.util.UUID;

public record RouteOptimizationResponse(
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
}
