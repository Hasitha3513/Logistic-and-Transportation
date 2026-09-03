package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.DeliverySlot;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservation;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DeliverySlotUseCase {
    record CreateSlotCommand(
            UUID deliveryZoneId,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime,
            DeliverySlotType slotType,
            int maxCapacity,
            OffsetDateTime cutoffTime,
            int bufferMinutes
    ) {}

    record UpdateSlotCommand(
            LocalTime startTime,
            LocalTime endTime,
            DeliverySlotType slotType,
            int maxCapacity,
            OffsetDateTime cutoffTime,
            int bufferMinutes,
            long expectedVersion
    ) {}

    record AssignSlotCommand(
            UUID deliveryOrderId,
            boolean isOverride,
            String overrideReason
    ) {}

    DeliverySlot createSlot(CreateSlotCommand command, String actor);
    DeliverySlot getSlot(UUID id);
    List<DeliverySlot> listSlots(UUID zoneId, LocalDate startDate, LocalDate endDate);
    List<DeliverySlot> getAvailableSlots(UUID deliveryZoneId, LocalDate date);
    DeliverySlot updateSlot(UUID id, UpdateSlotCommand command, String actor);
    DeliverySlot activateSlot(UUID id, long expectedVersion, String actor);
    DeliverySlot deactivateSlot(UUID id, long expectedVersion, String actor);
    DeliverySlot closeSlot(UUID id, long expectedVersion, String actor);

    DeliverySlotReservation assignDeliveryOrder(UUID slotId, AssignSlotCommand command, String actor);
    DeliverySlotReservation releaseReservation(UUID slotId, UUID deliveryOrderId, String actor);
    DeliverySlotReservation reassignDeliveryOrder(UUID newSlotId, UUID deliveryOrderId, boolean isOverride, String overrideReason, String actor);
    List<DeliverySlotReservation> listReservations(UUID slotId);
}
