package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliverySlotType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;
import java.time.OffsetDateTime;

public record UpdateDeliverySlotRequest(
        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        DeliverySlotType slotType,

        @Positive(message = "Max capacity must be greater than zero")
        int maxCapacity,

        OffsetDateTime cutoffTime,

        int bufferMinutes,

        long expectedVersion
) {}
