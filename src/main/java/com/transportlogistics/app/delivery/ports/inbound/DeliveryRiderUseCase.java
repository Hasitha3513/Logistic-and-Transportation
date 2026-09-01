package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderAvailability;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.domain.model.DeliveryTransportMode;
import com.transportlogistics.app.delivery.ports.outbound.DriverEligibilityPort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DeliveryRiderUseCase {

    DeliveryRider onboardRider(OnboardRiderCommand command, String actor);

    DeliveryRider updateRider(UUID id, UpdateRiderCommand command, String actor);

    DeliveryRider activateRider(UUID id, String actor);

    DeliveryRider deactivateRider(UUID id, String actor);

    DeliveryRider suspendRider(UUID id, String actor);

    DeliveryRider getRider(UUID id);

    List<DeliveryRiderSummary> listRiders(UUID zoneId, DeliveryRiderStatus status, DeliveryRiderType riderType);

    // Shifts
    DeliveryRiderShift createShift(UUID riderId, CreateShiftCommand command, String actor);

    List<DeliveryRiderShift> listShifts(UUID riderId);

    DeliveryRiderShift updateDutyStatus(UUID riderId, UUID shiftId, DutyStatusCommand command, String actor);

    // Order Assignment
    DeliveryOrderRiderAssignment assignRider(UUID deliveryOrderId, AssignRiderCommand command, String actor);

    DeliveryOrderRiderAssignment reassignRider(UUID deliveryOrderId, ReassignRiderCommand command, String actor);

    void unassignRider(UUID deliveryOrderId, String actor);

    List<DeliveryOrderRiderAssignment> getAssignmentHistory(UUID deliveryOrderId);

    List<DeliveryRiderSummary> queryAvailableRiders(UUID zoneId, LocalDate date, UUID slotId);

    record OnboardRiderCommand(
            String riderCode,
            UUID driverId,
            DeliveryRiderType riderType,
            DeliveryTransportMode transportMode,
            UUID primaryZoneId,
            Set<UUID> secondaryZoneIds,
            int maxConcurrentDeliveries
    ) {
    }

    record UpdateRiderCommand(
            UUID primaryZoneId,
            DeliveryTransportMode transportMode,
            Set<UUID> secondaryZoneIds,
            int maxConcurrentDeliveries
    ) {
    }

    record CreateShiftCommand(
            LocalDate shiftDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID deliverySlotId,
            int maxDeliveries
    ) {
    }

    record DutyStatusCommand(
            String action // "START_DUTY", "COMPLETE_DUTY", "CANCEL_SHIFT"
    ) {
    }

    record AssignRiderCommand(
            UUID riderId,
            boolean isOverride,
            String overrideReason
    ) {
    }

    record ReassignRiderCommand(
            UUID newRiderId,
            boolean isOverride,
            String overrideReason
    ) {
    }

    record DeliveryRiderSummary(
            UUID id,
            String riderCode,
            UUID driverId,
            DriverEligibilityPort.DriverSummary driverSummary,
            DeliveryRiderType riderType,
            DeliveryTransportMode transportMode,
            DeliveryRiderStatus status,
            DeliveryRiderAvailability availability,
            UUID primaryZoneId,
            Set<UUID> secondaryZoneIds,
            int activeWorkload,
            int maxConcurrentDeliveries,
            Optional<DeliveryRiderShift> currentShift
    ) {
    }
}
