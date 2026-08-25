package com.transportlogistics.app.routing.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record RouteDisruption(
        UUID id,
        UUID routeId,
        RouteDisruptionType disruptionType,
        DisruptionSeverity severity,
        String description,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveUntil,
        UUID detourRouteId,
        DisruptionStatus status,
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime resolvedAt,
        String resolvedBy
) {
    public RouteDisruption {
        Objects.requireNonNull(id, "Disruption id is required");
        Objects.requireNonNull(routeId, "Route id is required");
        Objects.requireNonNull(disruptionType, "Disruption type is required");
        Objects.requireNonNull(severity, "Severity is required");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        description = description.trim();
        Objects.requireNonNull(effectiveFrom, "Effective from timestamp is required");
        if (effectiveUntil != null && !effectiveUntil.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException("Effective until must be after effective from");
        }
        if (detourRouteId != null && detourRouteId.equals(routeId)) {
            throw new IllegalArgumentException("Detour route cannot be the same as the disrupted route");
        }
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(createdAt, "Created at timestamp is required");
        Objects.requireNonNull(createdBy, "Created by is required");
    }

    public static RouteDisruption create(
            UUID routeId,
            RouteDisruptionType disruptionType,
            DisruptionSeverity severity,
            String description,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveUntil,
            UUID detourRouteId,
            OffsetDateTime createdAt,
            String createdBy
    ) {
        return new RouteDisruption(
                UUID.randomUUID(),
                routeId,
                disruptionType,
                severity,
                description,
                effectiveFrom,
                effectiveUntil,
                detourRouteId,
                DisruptionStatus.ACTIVE,
                createdAt,
                createdBy != null && !createdBy.isBlank() ? createdBy.trim() : "system",
                null,
                null
        );
    }

    public RouteDisruption resolve(OffsetDateTime resolvedAt, String resolvedBy) {
        if (status == DisruptionStatus.RESOLVED) {
            throw new IllegalStateException("Disruption is already resolved");
        }
        Objects.requireNonNull(resolvedAt, "Resolved timestamp is required");
        return new RouteDisruption(
                id,
                routeId,
                disruptionType,
                severity,
                description,
                effectiveFrom,
                effectiveUntil,
                detourRouteId,
                DisruptionStatus.RESOLVED,
                createdAt,
                createdBy,
                resolvedAt,
                resolvedBy != null && !resolvedBy.isBlank() ? resolvedBy.trim() : "system"
        );
    }
}
