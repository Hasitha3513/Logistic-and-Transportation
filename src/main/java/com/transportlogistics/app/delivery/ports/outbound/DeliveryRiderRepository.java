package com.transportlogistics.app.delivery.ports.outbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRiderRepository {

    DeliveryRider save(DeliveryRider rider);

    Optional<DeliveryRider> findById(UUID id, UUID tenantId);

    Optional<DeliveryRider> findByIdForUpdate(UUID id, UUID tenantId);

    Optional<DeliveryRider> findByRiderCode(String riderCode, UUID tenantId);

    Optional<DeliveryRider> findActiveByDriverId(UUID driverId, UUID tenantId);

    boolean existsByRiderCode(String riderCode, UUID tenantId);

    boolean existsActiveByDriverId(UUID driverId, UUID tenantId);

    List<DeliveryRider> findAll(UUID tenantId, UUID zoneId, DeliveryRiderStatus status, DeliveryRiderType riderType);

    // Shifts
    DeliveryRiderShift saveShift(DeliveryRiderShift shift);

    Optional<DeliveryRiderShift> findShiftById(UUID shiftId, UUID tenantId);

    List<DeliveryRiderShift> findShiftsByRiderId(UUID riderId, UUID tenantId);

    List<DeliveryRiderShift> findActiveShiftsByRiderIdAndDate(UUID riderId, LocalDate date, UUID tenantId);

    // Assignments
    DeliveryOrderRiderAssignment saveAssignment(DeliveryOrderRiderAssignment assignment);

    Optional<DeliveryOrderRiderAssignment> findAssignmentById(UUID assignmentId, UUID tenantId);

    Optional<DeliveryOrderRiderAssignment> findActiveAssignmentForOrder(UUID deliveryOrderId, UUID tenantId);

    List<DeliveryOrderRiderAssignment> findActiveAssignmentsForRider(UUID riderId, UUID tenantId);

    List<DeliveryOrderRiderAssignment> findAssignmentHistoryForOrder(UUID deliveryOrderId, UUID tenantId);

    int countActiveAssignmentsForRider(UUID riderId, UUID tenantId);
}
