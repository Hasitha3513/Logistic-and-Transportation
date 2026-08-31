package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.RedeliveryScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRedeliveryScheduleJpaRepository extends JpaRepository<DeliveryRedeliveryScheduleEntity, UUID> {

    Optional<DeliveryRedeliveryScheduleEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DeliveryRedeliveryScheduleEntity> findByTenantIdAndDeliveryOrderIdOrderByCreatedAtDesc(UUID tenantId, UUID deliveryOrderId);

    Optional<DeliveryRedeliveryScheduleEntity> findFirstByTenantIdAndDeliveryOrderIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, UUID deliveryOrderId, RedeliveryScheduleStatus status
    );

    @Query("SELECT COUNT(s) FROM DeliveryRedeliveryScheduleEntity s " +
           "WHERE s.tenantId = :tenantId " +
           "AND s.status = com.transportlogistics.app.delivery.domain.model.RedeliveryScheduleStatus.CONFIRMED " +
           "AND s.scheduledStartTime < :endTime " +
           "AND s.scheduledEndTime > :startTime " +
           "AND (:excludeScheduleId IS NULL OR s.id != :excludeScheduleId)")
    int countActiveOverlapping(
            @Param("tenantId") UUID tenantId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("excludeScheduleId") UUID excludeScheduleId
    );
}
