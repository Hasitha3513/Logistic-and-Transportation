package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRiderShiftJpaRepository extends JpaRepository<DeliveryRiderShiftEntity, UUID> {

    Optional<DeliveryRiderShiftEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DeliveryRiderShiftEntity> findByRiderIdAndTenantIdOrderByShiftDateDescStartTimeDesc(UUID riderId, UUID tenantId);

    @Query("SELECT s FROM DeliveryRiderShiftEntity s WHERE s.riderId = :riderId AND s.tenantId = :tenantId AND s.shiftDate = :date AND s.status IN ('SCHEDULED', 'ON_DUTY')")
    List<DeliveryRiderShiftEntity> findActiveShiftsByRiderIdAndDate(
            @Param("riderId") UUID riderId,
            @Param("date") LocalDate date,
            @Param("tenantId") UUID tenantId
    );
}
