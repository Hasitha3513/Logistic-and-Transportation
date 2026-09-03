package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryOrderRiderAssignment;
import com.transportlogistics.app.delivery.domain.model.DeliveryRider;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderShift;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryRiderRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeliveryRiderPersistenceAdapter implements DeliveryRiderRepository {

    private final DeliveryRiderJpaRepository riderJpaRepository;
    private final DeliveryRiderShiftJpaRepository shiftJpaRepository;
    private final DeliveryOrderRiderAssignmentJpaRepository assignmentJpaRepository;

    public DeliveryRiderPersistenceAdapter(
            DeliveryRiderJpaRepository riderJpaRepository,
            DeliveryRiderShiftJpaRepository shiftJpaRepository,
            DeliveryOrderRiderAssignmentJpaRepository assignmentJpaRepository
    ) {
        this.riderJpaRepository = riderJpaRepository;
        this.shiftJpaRepository = shiftJpaRepository;
        this.assignmentJpaRepository = assignmentJpaRepository;
    }

    @Override
    public DeliveryRider save(DeliveryRider rider) {
        DeliveryRiderEntity entity = DeliveryRiderEntity.fromDomain(rider);
        return riderJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<DeliveryRider> findById(UUID id, UUID tenantId) {
        return riderJpaRepository.findByIdAndTenantId(id, tenantId).map(DeliveryRiderEntity::toDomain);
    }

    @Override
    public Optional<DeliveryRider> findByIdForUpdate(UUID id, UUID tenantId) {
        return riderJpaRepository.findByIdAndTenantIdWithLock(id, tenantId).map(DeliveryRiderEntity::toDomain);
    }

    @Override
    public Optional<DeliveryRider> findByRiderCode(String riderCode, UUID tenantId) {
        return riderJpaRepository.findByRiderCodeAndTenantId(riderCode, tenantId).map(DeliveryRiderEntity::toDomain);
    }

    @Override
    public Optional<DeliveryRider> findActiveByDriverId(UUID driverId, UUID tenantId) {
        return riderJpaRepository.findActiveByDriverIdAndTenantId(driverId, tenantId).map(DeliveryRiderEntity::toDomain);
    }

    @Override
    public boolean existsByRiderCode(String riderCode, UUID tenantId) {
        return riderJpaRepository.existsByRiderCodeAndTenantId(riderCode, tenantId);
    }

    @Override
    public boolean existsActiveByDriverId(UUID driverId, UUID tenantId) {
        return riderJpaRepository.existsActiveByDriverIdAndTenantId(driverId, tenantId);
    }

    @Override
    public List<DeliveryRider> findAll(UUID tenantId, UUID zoneId, DeliveryRiderStatus status, DeliveryRiderType riderType) {
        return riderJpaRepository.findRiders(tenantId, zoneId, status, riderType).stream()
                .map(DeliveryRiderEntity::toDomain)
                .toList();
    }

    @Override
    public DeliveryRiderShift saveShift(DeliveryRiderShift shift) {
        DeliveryRiderShiftEntity entity = DeliveryRiderShiftEntity.fromDomain(shift);
        return shiftJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<DeliveryRiderShift> findShiftById(UUID shiftId, UUID tenantId) {
        return shiftJpaRepository.findByIdAndTenantId(shiftId, tenantId).map(DeliveryRiderShiftEntity::toDomain);
    }

    @Override
    public List<DeliveryRiderShift> findShiftsByRiderId(UUID riderId, UUID tenantId) {
        return shiftJpaRepository.findByRiderIdAndTenantIdOrderByShiftDateDescStartTimeDesc(riderId, tenantId).stream()
                .map(DeliveryRiderShiftEntity::toDomain)
                .toList();
    }

    @Override
    public List<DeliveryRiderShift> findActiveShiftsByRiderIdAndDate(UUID riderId, LocalDate date, UUID tenantId) {
        return shiftJpaRepository.findActiveShiftsByRiderIdAndDate(riderId, date, tenantId).stream()
                .map(DeliveryRiderShiftEntity::toDomain)
                .toList();
    }

    @Override
    public DeliveryOrderRiderAssignment saveAssignment(DeliveryOrderRiderAssignment assignment) {
        DeliveryOrderRiderAssignmentEntity entity = DeliveryOrderRiderAssignmentEntity.fromDomain(assignment);
        return assignmentJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<DeliveryOrderRiderAssignment> findAssignmentById(UUID assignmentId, UUID tenantId) {
        return assignmentJpaRepository.findByIdAndTenantId(assignmentId, tenantId).map(DeliveryOrderRiderAssignmentEntity::toDomain);
    }

    @Override
    public Optional<DeliveryOrderRiderAssignment> findActiveAssignmentForOrder(UUID deliveryOrderId, UUID tenantId) {
        return assignmentJpaRepository.findActiveAssignmentForOrder(deliveryOrderId, tenantId).map(DeliveryOrderRiderAssignmentEntity::toDomain);
    }

    @Override
    public List<DeliveryOrderRiderAssignment> findActiveAssignmentsForRider(UUID riderId, UUID tenantId) {
        return assignmentJpaRepository.findActiveAssignmentsForRider(riderId, tenantId).stream()
                .map(DeliveryOrderRiderAssignmentEntity::toDomain)
                .toList();
    }

    @Override
    public List<DeliveryOrderRiderAssignment> findAssignmentHistoryForOrder(UUID deliveryOrderId, UUID tenantId) {
        return assignmentJpaRepository.findByDeliveryOrderIdAndTenantIdOrderByAssignedAtDesc(deliveryOrderId, tenantId).stream()
                .map(DeliveryOrderRiderAssignmentEntity::toDomain)
                .toList();
    }

    @Override
    public int countActiveAssignmentsForRider(UUID riderId, UUID tenantId) {
        return assignmentJpaRepository.countActiveAssignmentsForRider(riderId, tenantId);
    }
}
