package com.transportlogistics.app.delivery.adapters.outbound.persistence;

import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryZoneJpaRepository extends JpaRepository<DeliveryZoneEntity, UUID> {

    Optional<DeliveryZoneEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT z FROM DeliveryZoneEntity z WHERE z.id = :id AND z.tenantId = :tenantId")
    Optional<DeliveryZoneEntity> findByIdAndTenantIdWithLock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Optional<DeliveryZoneEntity> findByZoneCodeAndTenantId(String zoneCode, UUID tenantId);

    boolean existsByZoneCodeAndTenantId(String zoneCode, UUID tenantId);

    @Query("SELECT z FROM DeliveryZoneEntity z WHERE z.tenantId = :tenantId AND z.status = 'ACTIVE' " +
           "AND z.minLatitude <= :lat AND z.maxLatitude >= :lat " +
           "AND z.minLongitude <= :lon AND z.maxLongitude >= :lon")
    List<DeliveryZoneEntity> findActiveCandidatesByBBox(
            @Param("tenantId") UUID tenantId,
            @Param("lat") double lat,
            @Param("lon") double lon
    );

    @Query("SELECT z FROM DeliveryZoneEntity z WHERE z.tenantId = :tenantId " +
           "AND (:status IS NULL OR z.status = :status) " +
           "AND (:serviceable IS NULL OR z.serviceable = :serviceable) " +
           "ORDER BY z.zoneCode ASC")
    List<DeliveryZoneEntity> findAllByTenant(
            @Param("tenantId") UUID tenantId,
            @Param("status") DeliveryZoneStatus status,
            @Param("serviceable") Boolean serviceable
    );
}
