package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateLoadPlanRequest(
        @NotNull(message = "Vehicle ID is required")
        UUID vehicleId,

        List<@Valid LoadPlanItemPlacementRequest> placements,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        @NotNull(message = "Version is required")
        @PositiveOrZero(message = "Version must be non-negative")
        Long version
) {
}
