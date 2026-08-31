package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryOrderRiderAssignmentJpaRepository extends JpaRepository<DeliveryOrderRiderAssignmentEntity, UUID> {

    Optional<DeliveryOrderRiderAssignmentEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT a FROM DeliveryOrderRiderAssignmentEntity a WHERE a.deliveryOrderId = :orderId AND a.tenantId = :tenantId AND a.status = 'ACTIVE'")
    Optional<DeliveryOrderRiderAssignmentEntity> findActiveAssignmentForOrder(
            @Param("orderId") UUID orderId,
            @Param("tenantId") UUID tenantId
    );

    List<DeliveryOrderRiderAssignmentEntity> findByDeliveryOrderIdAndTenantIdOrderByAssignedAtDesc(UUID deliveryOrderId, UUID tenantId);

    @Query("SELECT COUNT(a) FROM DeliveryOrderRiderAssignmentEntity a WHERE a.riderId = :riderId AND a.tenantId = :tenantId AND a.status = 'ACTIVE'")
    int countActiveAssignmentsForRider(
            @Param("riderId") UUID riderId,
            @Param("tenantId") UUID tenantId
    );
}
