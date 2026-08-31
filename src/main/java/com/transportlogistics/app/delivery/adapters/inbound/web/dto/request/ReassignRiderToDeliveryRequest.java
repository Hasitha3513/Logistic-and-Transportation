package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReassignRiderToDeliveryRequest(
        @NotNull UUID newRiderId,
        boolean isOverride,
        String overrideReason
) {
}
