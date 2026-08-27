package com.transportlogistics.app.routing.domain.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Route(UUID id, String code, String name, UUID originLocationId, UUID destinationLocationId,
                    Double plannedDistanceKm, Integer estimatedDurationMinutes, boolean active,
                    List<UUID> stopLocationIds) {
    public Route {
        Objects.requireNonNull(id, "Route id is required");
        code = requireText(code, 40, "Route code is required");
        name = requireText(name, 160, "Route name is required");
        Objects.requireNonNull(originLocationId, "Route origin is required");
        Objects.requireNonNull(destinationLocationId, "Route destination is required");
        if (originLocationId.equals(destinationLocationId)) {
            throw new IllegalArgumentException("Route origin and destination must be different");
        }
        if (plannedDistanceKm == null || !Double.isFinite(plannedDistanceKm) || plannedDistanceKm <= 0) {
            throw new IllegalArgumentException("Planned distance must be greater than zero");
        }
        if (estimatedDurationMinutes == null || estimatedDurationMinutes <= 0) {
            throw new IllegalArgumentException("Estimated duration must be greater than zero");
        }
        stopLocationIds = stopLocationIds == null ? List.of() : List.copyOf(stopLocationIds);
        if (stopLocationIds.size() > 50) {
            throw new IllegalArgumentException("A route cannot contain more than 50 stops");
        }
        if (stopLocationIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Route stops cannot contain null locations");
        }
        if (new LinkedHashSet<>(stopLocationIds).size() != stopLocationIds.size()) {
            throw new IllegalArgumentException("Route stops must be unique");
        }
        if (stopLocationIds.contains(originLocationId) || stopLocationIds.contains(destinationLocationId)) {
            throw new IllegalArgumentException("Route stops cannot repeat the origin or destination");
        }
    }

    private static String requireText(String value, int maximumLength, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        var normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(message.replace(" is required", " exceeds " + maximumLength + " characters"));
        }
        return normalized;
    }
}
