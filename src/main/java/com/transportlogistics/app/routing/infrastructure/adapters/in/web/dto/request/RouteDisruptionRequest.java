package com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.routing.domain.model.DisruptionSeverity;
import com.transportlogistics.app.routing.domain.model.RouteDisruptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RouteDisruptionRequest(
        @NotNull(message = "Disruption type is required")
        RouteDisruptionType disruptionType,
        @NotNull(message = "Severity is required")
        DisruptionSeverity severity,
        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,
        @NotNull(message = "Effective from timestamp is required")
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveUntil,
        UUID detourRouteId
) {}
