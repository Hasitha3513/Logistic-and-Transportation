package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryPriority;
import com.transportlogistics.app.delivery.domain.model.DeliveryServiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateDeliveryOrderRequest(
        @PositiveOrZero long version,
        @NotNull UUID customerId,
        @NotNull UUID originLocationId,
        @NotNull UUID destinationLocationId,
        @NotNull DeliveryPriority priority,
        @NotNull DeliveryServiceType serviceType,
        @NotNull OffsetDateTime windowStart,
        @NotNull OffsetDateTime windowEnd,
        String instructions
) {}
