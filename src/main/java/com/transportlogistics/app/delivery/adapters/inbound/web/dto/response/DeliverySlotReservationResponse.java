package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservation;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliverySlotReservationResponse(
        UUID id,
        UUID tenantId,
        UUID deliverySlotId,
        UUID deliveryOrderId,
        DeliverySlotReservationStatus status,
        OffsetDateTime reservedAt,
        String reservedBy,
        OffsetDateTime releasedAt,
        String releasedBy,
        boolean isOverride,
        String overrideReason,
        long version
) {
    public static DeliverySlotReservationResponse fromDomain(DeliverySlotReservation reservation) {
        return new DeliverySlotReservationResponse(
                reservation.getId(),
                reservation.getTenantId(),
                reservation.getDeliverySlotId(),
                reservation.getDeliveryOrderId(),
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getReservedBy(),
                reservation.getReleasedAt(),
                reservation.getReleasedBy(),
                reservation.isOverride(),
                reservation.getOverrideReason(),
                reservation.getVersion()
        );
    }
}
