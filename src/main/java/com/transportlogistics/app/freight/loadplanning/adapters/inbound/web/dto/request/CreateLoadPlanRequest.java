package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateLoadPlanRequest(
        @NotNull(message = "Cargo manifest ID is required")
        UUID cargoManifestId,

        @NotNull(message = "Vehicle ID is required")
        UUID vehicleId,

        List<@Valid LoadPlanItemPlacementRequest> placements,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {
}
