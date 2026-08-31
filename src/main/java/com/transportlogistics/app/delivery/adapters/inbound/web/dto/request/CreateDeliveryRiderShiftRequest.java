package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateDeliveryRiderShiftRequest(
        @NotNull LocalDate shiftDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        UUID deliverySlotId,
        Integer maxDeliveries
) {
}
