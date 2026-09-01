package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryBatchOrderJpaRepository extends JpaRepository<DeliveryBatchOrderEntity, UUID> {

    @Query("""
        SELECT o FROM DeliveryBatchOrderEntity o
        WHERE o.tenantId = :tenantId AND o.batchId = :batchId AND o.status = 'ACTIVE'
        ORDER BY o.sequenceHint ASC NULLS LAST, o.addedAt ASC
    """)
    List<DeliveryBatchOrderEntity> findActiveByBatchId(@Param("tenantId") UUID tenantId, @Param("batchId") UUID batchId);

    @Query("""
        SELECT o FROM DeliveryBatchOrderEntity o
        WHERE o.tenantId = :tenantId AND o.batchId = :batchId
        ORDER BY o.addedAt ASC
    """)
    List<DeliveryBatchOrderEntity> findAllByBatchId(@Param("tenantId") UUID tenantId, @Param("batchId") UUID batchId);

    Optional<DeliveryBatchOrderEntity> findByTenantIdAndDeliveryOrderIdAndStatus(UUID tenantId, UUID deliveryOrderId, String status);

    @Query("""
        SELECT o.deliveryOrderId FROM DeliveryBatchOrderEntity o
        WHERE o.tenantId = :tenantId AND o.status = 'ACTIVE' AND o.deliveryOrderId IN :deliveryOrderIds
    """)
    List<UUID> findActiveBatchedDeliveryOrderIds(@Param("tenantId") UUID tenantId, @Param("deliveryOrderIds") List<UUID> deliveryOrderIds);

    @Query("SELECT COUNT(o) FROM DeliveryBatchOrderEntity o WHERE o.tenantId = :tenantId AND o.batchId = :batchId AND o.status = 'ACTIVE'")
    int countActiveByBatchId(@Param("tenantId") UUID tenantId, @Param("batchId") UUID batchId);
}
