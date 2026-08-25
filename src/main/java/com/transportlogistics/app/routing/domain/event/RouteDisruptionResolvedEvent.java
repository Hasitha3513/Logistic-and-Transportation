package com.transportlogistics.app.routing.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RouteDisruptionResolvedEvent(
        UUID disruptionId,
        UUID routeId,
        OffsetDateTime resolvedAt,
        String resolvedBy
) {}
