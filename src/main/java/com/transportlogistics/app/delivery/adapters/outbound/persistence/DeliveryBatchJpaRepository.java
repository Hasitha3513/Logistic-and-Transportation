package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryBatchJpaRepository extends JpaRepository<DeliveryBatchEntity, UUID> {

    Optional<DeliveryBatchEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DeliveryBatchEntity b WHERE b.id = :id AND b.tenantId = :tenantId")
    Optional<DeliveryBatchEntity> findByIdAndTenantIdForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("""
        SELECT b FROM DeliveryBatchEntity b
        WHERE b.tenantId = :tenantId
          AND (:zoneId IS NULL OR b.deliveryZoneId = :zoneId)
          AND (:slotId IS NULL OR b.deliverySlotId = :slotId)
          AND (:riderId IS NULL OR b.riderId = :riderId)
          AND (:status IS NULL OR b.status = :status)
        ORDER BY b.createdAt DESC
    """)
    List<DeliveryBatchEntity> findBatches(
            @Param("tenantId") UUID tenantId,
            @Param("zoneId") UUID zoneId,
            @Param("slotId") UUID slotId,
            @Param("riderId") UUID riderId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(b) FROM DeliveryBatchEntity b
        WHERE b.tenantId = :tenantId
          AND (:zoneId IS NULL OR b.deliveryZoneId = :zoneId)
          AND (:slotId IS NULL OR b.deliverySlotId = :slotId)
          AND (:riderId IS NULL OR b.riderId = :riderId)
          AND (:status IS NULL OR b.status = :status)
    """)
    long countBatches(
            @Param("tenantId") UUID tenantId,
            @Param("zoneId") UUID zoneId,
            @Param("slotId") UUID slotId,
            @Param("riderId") UUID riderId,
            @Param("status") String status
    );
}
