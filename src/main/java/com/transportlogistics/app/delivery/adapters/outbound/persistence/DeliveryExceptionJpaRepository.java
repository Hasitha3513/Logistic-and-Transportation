package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryExceptionJpaRepository extends JpaRepository<DeliveryExceptionCaseEntity, UUID> {
    Optional<DeliveryExceptionCaseEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DeliveryExceptionCaseEntity> findByTenantIdAndDeliveryOrderIdOrderByReportedAtDesc(UUID tenantId, UUID deliveryOrderId);

    boolean existsByTenantIdAndDeliveryOrderIdAndExceptionTypeAndStatusIn(
            UUID tenantId, UUID deliveryOrderId, DeliveryExceptionType exceptionType, Collection<DeliveryExceptionStatus> statuses);

    @Query("SELECT COUNT(e) > 0 FROM DeliveryExceptionCaseEntity e " +
           "WHERE e.tenantId = :tenantId AND e.deliveryOrderId = :deliveryOrderId " +
           "AND e.status IN ('OPEN', 'UNDER_INVESTIGATION') " +
           "AND e.exceptionType IN ('OTP_MISMATCH', 'DAMAGED_DELIVERY')")
    boolean hasActiveBlockingExceptions(@Param("tenantId") UUID tenantId, @Param("deliveryOrderId") UUID deliveryOrderId);
}
