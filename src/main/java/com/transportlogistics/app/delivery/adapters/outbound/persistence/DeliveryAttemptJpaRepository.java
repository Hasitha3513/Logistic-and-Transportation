package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAttemptJpaRepository extends JpaRepository<DeliveryAttemptEntity, UUID> {
    Optional<DeliveryAttemptEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DeliveryAttemptEntity> findByDeliveryIdAndTenantIdOrderByAttemptNumberAsc(UUID deliveryId, UUID tenantId);

    int countByDeliveryIdAndTenantId(UUID deliveryId, UUID tenantId);

    Optional<DeliveryAttemptEntity> findFirstByDeliveryIdAndTenantIdOrderByAttemptNumberDesc(UUID deliveryId, UUID tenantId);
}
