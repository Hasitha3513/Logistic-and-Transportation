package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShiftStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryRiderShiftResponse(
        UUID id,
        UUID riderId,
        LocalDate shiftDate,
        LocalTime startTime,
        LocalTime endTime,
        UUID deliverySlotId,
        DeliveryRiderShiftStatus status,
        int maxDeliveries,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public static DeliveryRiderShiftResponse from(DeliveryRiderShift domain) {
        return new DeliveryRiderShiftResponse(
                domain.getId(),
                domain.getRiderId(),
                domain.getShiftDate(),
                domain.getStartTime(),
                domain.getEndTime(),
                domain.getDeliverySlotId(),
                domain.getStatus(),
                domain.getMaxDeliveries(),
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getCreatedBy(),
                domain.getUpdatedBy()
        );
    }
}
