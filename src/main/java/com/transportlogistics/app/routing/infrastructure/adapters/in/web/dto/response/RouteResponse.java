package com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response;

import java.util.List;
import java.util.UUID;

public record RouteResponse(UUID id,
                            String code,
                            String name,
                            UUID originLocationId,
                            UUID destinationLocationId,
                            Double plannedDistanceKm,
                            Integer estimatedDurationMinutes,
                            boolean active,
                            List<UUID> stopLocationIds) {
}
