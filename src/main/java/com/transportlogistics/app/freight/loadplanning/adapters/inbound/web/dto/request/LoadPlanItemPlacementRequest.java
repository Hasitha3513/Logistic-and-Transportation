package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LoadPlanItemPlacementRequest(
        @NotNull(message = "Manifest item ID is required")
        UUID manifestItemId,

        @NotNull(message = "Placement order is required")
        @PositiveOrZero(message = "Placement order must be non-negative")
        Integer placementOrder,

        @Size(max = 120, message = "Zone reference must not exceed 120 characters")
        String zoneReference,

        @Size(max = 120, message = "Stack group must not exceed 120 characters")
        String stackGroup,

        @Size(max = 200, message = "Container reference must not exceed 200 characters")
        String containerReference,

        @NotNull(message = "Loading sequence is required")
        @PositiveOrZero(message = "Loading sequence must be non-negative")
        Integer loadingSequence,

        @Size(max = 500, message = "Special handling notes must not exceed 500 characters")
        String specialHandlingNotes
) {
}
