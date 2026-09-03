package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryRiderStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryRiderType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRiderJpaRepository extends JpaRepository<DeliveryRiderEntity, UUID> {

    Optional<DeliveryRiderEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DeliveryRiderEntity r WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<DeliveryRiderEntity> findByIdAndTenantIdWithLock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Optional<DeliveryRiderEntity> findByRiderCodeAndTenantId(String riderCode, UUID tenantId);

    @Query("SELECT r FROM DeliveryRiderEntity r WHERE r.driverId = :driverId AND r.tenantId = :tenantId AND r.status = 'ACTIVE'")
    Optional<DeliveryRiderEntity> findActiveByDriverIdAndTenantId(@Param("driverId") UUID driverId, @Param("tenantId") UUID tenantId);

    boolean existsByRiderCodeAndTenantId(String riderCode, UUID tenantId);

    @Query("SELECT COUNT(r) > 0 FROM DeliveryRiderEntity r WHERE r.driverId = :driverId AND r.tenantId = :tenantId AND r.status = 'ACTIVE'")
    boolean existsActiveByDriverIdAndTenantId(@Param("driverId") UUID driverId, @Param("tenantId") UUID tenantId);

    @Query("SELECT DISTINCT r FROM DeliveryRiderEntity r LEFT JOIN r.secondaryZoneIds sz " +
           "WHERE r.tenantId = :tenantId " +
           "AND (:zoneId IS NULL OR r.primaryZoneId = :zoneId OR sz = :zoneId) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:riderType IS NULL OR r.riderType = :riderType)")
    List<DeliveryRiderEntity> findRiders(
            @Param("tenantId") UUID tenantId,
            @Param("zoneId") UUID zoneId,
            @Param("status") DeliveryRiderStatus status,
            @Param("riderType") DeliveryRiderType riderType
    );
}
