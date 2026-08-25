package com.transportlogistics.app.routing.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RouteRevision(
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
) {
    public RouteRevision {
        Objects.requireNonNull(id, "Revision id is required");
        Objects.requireNonNull(routeId, "Route id is required");
        if (revisionNumber <= 0) {
            throw new IllegalArgumentException("Revision number must be positive");
        }
        Objects.requireNonNull(code, "Route code is required");
        Objects.requireNonNull(name, "Route name is required");
        Objects.requireNonNull(originLocationId, "Origin is required");
        Objects.requireNonNull(destinationLocationId, "Destination is required");
        Objects.requireNonNull(plannedDistanceKm, "Planned distance is required");
        Objects.requireNonNull(estimatedDurationMinutes, "Estimated duration is required");
        Objects.requireNonNull(changedAt, "Changed timestamp is required");
        Objects.requireNonNull(changedBy, "Changed by is required");
        stopLocationIds = stopLocationIds == null ? List.of() : List.copyOf(stopLocationIds);
    }

    public static RouteRevision from(Route route, int revisionNumber, OffsetDateTime changedAt, String changedBy) {
        return new RouteRevision(
                UUID.randomUUID(),
                route.id(),
                revisionNumber,
                route.code(),
                route.name(),
                route.originLocationId(),
                route.destinationLocationId(),
                route.plannedDistanceKm(),
                route.estimatedDurationMinutes(),
                route.active(),
                route.stopLocationIds(),
                changedAt,
                changedBy != null && !changedBy.isBlank() ? changedBy.trim() : "system"
        );
    }
}
