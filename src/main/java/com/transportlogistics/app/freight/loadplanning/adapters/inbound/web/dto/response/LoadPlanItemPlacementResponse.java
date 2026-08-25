package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response;

import java.util.UUID;

public record LoadPlanItemPlacementResponse(
        UUID id,
        UUID manifestItemId,
        int placementOrder,
        String zoneReference,
        String stackGroup,
        String containerReference,
        int loadingSequence,
        String specialHandlingNotes
) {
}
