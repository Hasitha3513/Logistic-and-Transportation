package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliverySlotType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateDeliverySlotRequest(
        @NotNull(message = "Delivery zone ID is required")
        UUID deliveryZoneId,

        @NotNull(message = "Slot date is required")
        LocalDate slotDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        DeliverySlotType slotType,

        @Positive(message = "Max capacity must be greater than zero")
        int maxCapacity,

        OffsetDateTime cutoffTime,

        int bufferMinutes
) {}
