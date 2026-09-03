package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliverySlot;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotStatus;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliverySlotResponse(
        UUID id,
        UUID tenantId,
        UUID deliveryZoneId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        DeliverySlotType slotType,
        int maxCapacity,
        int reservedCapacity,
        int remainingCapacity,
        OffsetDateTime cutoffTime,
        int bufferMinutes,
        DeliverySlotStatus status,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public static DeliverySlotResponse fromDomain(DeliverySlot slot) {
        return new DeliverySlotResponse(
                slot.getId(),
                slot.getTenantId(),
                slot.getDeliveryZoneId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getSlotType(),
                slot.getMaxCapacity(),
                slot.getReservedCapacity(),
                slot.getRemainingCapacity(),
                slot.getCutoffTime(),
                slot.getBufferMinutes(),
                slot.getStatus(),
                slot.getVersion(),
                slot.getCreatedAt(),
                slot.getUpdatedAt(),
                slot.getCreatedBy(),
                slot.getUpdatedBy()
        );
    }
}
