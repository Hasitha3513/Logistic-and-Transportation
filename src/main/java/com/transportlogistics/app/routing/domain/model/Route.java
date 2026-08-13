package com.transportlogistics.app.routing.domain.model;

import java.util.UUID;

public record Route(UUID id, String code, String name, UUID originLocationId, UUID destinationLocationId,
                    Double plannedDistanceKm, Integer estimatedDurationMinutes, boolean active) {
}
