package com.transportlogistics.app.routing.domain.event;

import com.transportlogistics.app.routing.domain.model.DisruptionSeverity;
import com.transportlogistics.app.routing.domain.model.RouteDisruptionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RouteDisruptionCreatedEvent(
        UUID disruptionId,
        UUID routeId,
        RouteDisruptionType disruptionType,
        DisruptionSeverity severity,
        UUID detourRouteId,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveUntil
) {}
