package com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.routing.domain.model.DisruptionSeverity;
import com.transportlogistics.app.routing.domain.model.DisruptionStatus;
import com.transportlogistics.app.routing.domain.model.RouteDisruptionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RouteDisruptionResponse(
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
) {}
