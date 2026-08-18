package com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RouteRequest(@NotBlank @Size(max = 40) String code,
                           @NotBlank @Size(max = 160) String name,
                           @NotNull UUID originLocationId,
                           @NotNull UUID destinationLocationId,
                           @NotNull @Positive Double plannedDistanceKm,
                           @NotNull @Positive Integer estimatedDurationMinutes,
                           @Size(max = 50) List<@NotNull UUID> stops,
                           Boolean active) {
}
