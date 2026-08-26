package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response;

import com.transportlogistics.app.freight.loadplanning.domain.LoadPlanReadinessStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LoadPlanResponse(
        UUID id,
        String loadPlanNumber,
        UUID cargoManifestId,
        UUID vehicleId,
        List<LoadPlanItemPlacementResponse> placements,
        String notes,
        LoadPlanReadinessStatus readinessStatus,
        OffsetDateTime readyAt,
        String readyBy,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
}
