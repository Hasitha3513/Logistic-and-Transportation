package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliverySlot;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservation;
import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservationStatus;
import com.transportlogistics.app.delivery.ports.outbound.DeliverySlotRepository;
import com.transportlogistics.app.delivery.ports.outbound.DeliveryTenantContextPort;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DeliverySlotPersistenceAdapter implements DeliverySlotRepository {
    private final DeliverySlotJpaRepository slotJpaRepository;
    private final DeliverySlotReservationJpaRepository reservationJpaRepository;
    private final DeliveryTenantContextPort tenantContext;

    public DeliverySlotPersistenceAdapter(
            DeliverySlotJpaRepository slotJpaRepository,
            DeliverySlotReservationJpaRepository reservationJpaRepository,
            DeliveryTenantContextPort tenantContext
    ) {
        this.slotJpaRepository = slotJpaRepository;
        this.reservationJpaRepository = reservationJpaRepository;
        this.tenantContext = tenantContext;
    }

    private UUID currentTenantId() {
        return tenantContext.currentTenantId()
                .orElseThrow(() -> new BusinessRuleException("TENANT_CONTEXT_REQUIRED", "Tenant context is required"));
    }

    @Override
    public DeliverySlot save(DeliverySlot slot) {
        DeliverySlotEntity entity = DeliverySlotEntity.fromDomain(slot);
        return slotJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<DeliverySlot> findById(UUID id) {
        return slotJpaRepository.findByIdAndTenantId(id, currentTenantId())
                .map(DeliverySlotEntity::toDomain);
    }

    @Override
    public Optional<DeliverySlot> findByIdForUpdate(UUID id) {
        return slotJpaRepository.findByIdAndTenantIdWithLock(id, currentTenantId())
                .map(DeliverySlotEntity::toDomain);
    }

    @Override
    public List<DeliverySlot> findByZoneAndDate(UUID zoneId, LocalDate date) {
        return slotJpaRepository.findByTenantIdAndDeliveryZoneIdAndSlotDate(currentTenantId(), zoneId, date).stream()
                .map(DeliverySlotEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliverySlot> findByZoneAndDateRange(UUID zoneId, LocalDate startDate, LocalDate endDate) {
        return slotJpaRepository.findByTenantIdAndDeliveryZoneIdAndSlotDateBetween(currentTenantId(), zoneId, startDate, endDate).stream()
                .map(DeliverySlotEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliverySlot> findByDate(LocalDate date) {
        return slotJpaRepository.findByTenantIdAndSlotDate(currentTenantId(), date).stream()
                .map(DeliverySlotEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsOverlapping(DeliverySlot slot) {
        return slotJpaRepository.existsOverlapping(
                slot.getTenantId(),
                slot.getDeliveryZoneId(),
                slot.getSlotDate(),
                slot.getSlotType(),
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime()
        );
    }

    @Override
    public int countActiveBookingsInZoneOnDate(UUID zoneId, LocalDate date) {
        return slotJpaRepository.countActiveBookingsInZoneOnDate(currentTenantId(), zoneId, date);
    }

    @Override
    public DeliverySlotReservation saveReservation(DeliverySlotReservation reservation) {
        DeliverySlotReservationEntity entity = DeliverySlotReservationEntity.fromDomain(reservation);
        return reservationJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<DeliverySlotReservation> findActiveReservationForOrder(UUID deliveryOrderId) {
        return reservationJpaRepository.findByTenantIdAndDeliveryOrderIdAndStatus(
                currentTenantId(), deliveryOrderId, DeliverySlotReservationStatus.ACTIVE
        ).map(DeliverySlotReservationEntity::toDomain);
    }

    @Override
    public List<DeliverySlotReservation> findReservationsBySlotId(UUID slotId) {
        return reservationJpaRepository.findByTenantIdAndDeliverySlotId(currentTenantId(), slotId).stream()
                .map(DeliverySlotReservationEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DeliverySlotReservation> findReservationById(UUID id) {
        return reservationJpaRepository.findByIdAndTenantId(id, currentTenantId())
                .map(DeliverySlotReservationEntity::toDomain);
    }
}
