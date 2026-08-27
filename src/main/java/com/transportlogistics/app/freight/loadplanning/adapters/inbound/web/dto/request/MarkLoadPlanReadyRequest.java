package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for transitioning a Load Plan to STRUCTURALLY_READY.
 */
public record MarkLoadPlanReadyRequest(
        @NotNull(message = "Version is required")
        Long version
) {
}
