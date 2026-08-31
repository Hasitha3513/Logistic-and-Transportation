package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliverySlotType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliverySlotJpaRepository extends JpaRepository<DeliverySlotEntity, UUID> {
    Optional<DeliverySlotEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DeliverySlotEntity s WHERE s.id = :id AND s.tenantId = :tenantId")
    Optional<DeliverySlotEntity> findByIdAndTenantIdWithLock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    List<DeliverySlotEntity> findByTenantIdAndDeliveryZoneIdAndSlotDate(UUID tenantId, UUID deliveryZoneId, LocalDate slotDate);

    List<DeliverySlotEntity> findByTenantIdAndDeliveryZoneIdAndSlotDateBetween(UUID tenantId, UUID deliveryZoneId, LocalDate startDate, LocalDate endDate);

    List<DeliverySlotEntity> findByTenantIdAndSlotDate(UUID tenantId, LocalDate slotDate);

    @Query("SELECT COUNT(s) > 0 FROM DeliverySlotEntity s WHERE s.tenantId = :tenantId " +
            "AND s.deliveryZoneId = :zoneId AND s.slotDate = :slotDate AND s.slotType = :slotType " +
            "AND s.status = 'ACTIVE' AND s.id <> :excludeId " +
            "AND s.startTime < :endTime AND :startTime < s.endTime")
    boolean existsOverlapping(
            @Param("tenantId") UUID tenantId,
            @Param("zoneId") UUID zoneId,
            @Param("slotDate") LocalDate slotDate,
            @Param("slotType") DeliverySlotType slotType,
            @Param("excludeId") UUID excludeId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("SELECT COALESCE(SUM(s.reservedCapacity), 0) FROM DeliverySlotEntity s WHERE s.tenantId = :tenantId " +
            "AND s.deliveryZoneId = :zoneId AND s.slotDate = :slotDate")
    int countActiveBookingsInZoneOnDate(
            @Param("tenantId") UUID tenantId,
            @Param("zoneId") UUID zoneId,
            @Param("slotDate") LocalDate slotDate
    );
}
