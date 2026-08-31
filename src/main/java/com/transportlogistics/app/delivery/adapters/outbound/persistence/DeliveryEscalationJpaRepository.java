package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryEscalationJpaRepository extends JpaRepository<DeliveryEscalationEntity, UUID> {
    Optional<DeliveryEscalationEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DeliveryEscalationEntity> findByDeliveryIdAndTenantIdOrderByEscalatedAtAsc(UUID deliveryId, UUID tenantId);

    Optional<DeliveryEscalationEntity> findFirstByDeliveryIdAndTenantIdOrderByEscalatedAtDesc(UUID deliveryId, UUID tenantId);

    List<DeliveryEscalationEntity> findByStatusAndTenantIdOrderByEscalatedAtDesc(String status, UUID tenantId);
}
