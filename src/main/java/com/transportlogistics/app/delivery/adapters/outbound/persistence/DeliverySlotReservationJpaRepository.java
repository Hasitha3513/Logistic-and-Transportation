package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliverySlotReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliverySlotReservationJpaRepository extends JpaRepository<DeliverySlotReservationEntity, UUID> {
    Optional<DeliverySlotReservationEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<DeliverySlotReservationEntity> findByTenantIdAndDeliveryOrderIdAndStatus(UUID tenantId, UUID deliveryOrderId, DeliverySlotReservationStatus status);

    List<DeliverySlotReservationEntity> findByTenantIdAndDeliverySlotId(UUID tenantId, UUID deliverySlotId);
}
