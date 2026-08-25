package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response;

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
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
}
