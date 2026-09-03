package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryRiderAvailability;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.ports.inbound.DeliveryRiderUseCase;

import java.util.Set;
import java.util.UUID;

public record DeliveryRiderSummaryResponse(
        UUID id,
        String riderCode,
        UUID driverId,
        String driverName,
        String driverEmployeeNumber,
        DeliveryRiderType riderType,
        DeliveryTransportMode transportMode,
        DeliveryRiderStatus status,
        DeliveryRiderAvailability availability,
        UUID primaryZoneId,
        Set<UUID> secondaryZoneIds,
        int activeWorkload,
        int maxConcurrentDeliveries,
        DeliveryRiderShiftResponse currentShift
) {
    public static DeliveryRiderSummaryResponse from(DeliveryRiderUseCase.DeliveryRiderSummary summary) {
        String name = summary.driverSummary() != null
                ? summary.driverSummary().firstName() + " " + summary.driverSummary().lastName()
                : "Unknown Driver";
        String empNum = summary.driverSummary() != null ? summary.driverSummary().employeeNumber() : null;

        return new DeliveryRiderSummaryResponse(
                summary.id(),
                summary.riderCode(),
                summary.driverId(),
                name,
                empNum,
                summary.riderType(),
                summary.transportMode(),
                summary.status(),
                summary.availability(),
                summary.primaryZoneId(),
                summary.secondaryZoneIds(),
                summary.activeWorkload(),
                summary.maxConcurrentDeliveries(),
                summary.currentShift().map(DeliveryRiderShiftResponse::from).orElse(null)
        );
    }
}
