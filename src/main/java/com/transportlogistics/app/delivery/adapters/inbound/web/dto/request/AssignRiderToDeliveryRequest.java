package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignRiderToDeliveryRequest(
        @NotNull UUID riderId,
        boolean isOverride,
        String overrideReason
) {
}
