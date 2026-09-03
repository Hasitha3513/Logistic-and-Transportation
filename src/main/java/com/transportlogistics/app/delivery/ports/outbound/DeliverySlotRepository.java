package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliverySlot;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliverySlotRepository {
    DeliverySlot save(DeliverySlot slot);
    Optional<DeliverySlot> findById(UUID id);
    Optional<DeliverySlot> findByIdForUpdate(UUID id);
    List<DeliverySlot> findByZoneAndDate(UUID zoneId, LocalDate date);
    List<DeliverySlot> findByZoneAndDateRange(UUID zoneId, LocalDate startDate, LocalDate endDate);
    List<DeliverySlot> findByDate(LocalDate date);
    boolean existsOverlapping(DeliverySlot slot);
    int countActiveBookingsInZoneOnDate(UUID zoneId, LocalDate date);

    DeliverySlotReservation saveReservation(DeliverySlotReservation reservation);
    Optional<DeliverySlotReservation> findActiveReservationForOrder(UUID deliveryOrderId);
    List<DeliverySlotReservation> findReservationsBySlotId(UUID slotId);
    Optional<DeliverySlotReservation> findReservationById(UUID id);
}
