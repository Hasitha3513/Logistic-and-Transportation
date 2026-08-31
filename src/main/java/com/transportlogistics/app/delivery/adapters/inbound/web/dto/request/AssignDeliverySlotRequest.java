package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDeliverySlotRequest(
        @NotNull(message = "Delivery order ID is required")
        UUID deliveryOrderId,

        boolean isOverride,

        String overrideReason
) {}
