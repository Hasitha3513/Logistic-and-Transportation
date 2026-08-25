package com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RouteRevisionResponse(
        UUID id,
        UUID routeId,
        int revisionNumber,
        String code,
        String name,
        UUID originLocationId,
        UUID destinationLocationId,
        Double plannedDistanceKm,
        Integer estimatedDurationMinutes,
        boolean active,
        List<UUID> stopLocationIds,
        OffsetDateTime changedAt,
        String changedBy
) {}
