package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryContactAttemptJpaRepository extends JpaRepository<DeliveryContactAttemptEntity, UUID> {
    Optional<DeliveryContactAttemptEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DeliveryContactAttemptEntity> findByDeliveryAttemptIdAndTenantIdOrderByContactTimestampAsc(UUID deliveryAttemptId, UUID tenantId);
}
