package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryPriority;
import com.transportlogistics.app.delivery.domain.model.DeliveryServiceType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateDeliveryOrderRequest(
        @NotNull UUID customerId,
        @NotNull UUID originLocationId,
        @NotNull UUID destinationLocationId,
        DeliveryPriority priority,
        DeliveryServiceType serviceType,
        @NotNull OffsetDateTime windowStart,
        @NotNull OffsetDateTime windowEnd,
        String instructions
) {}
