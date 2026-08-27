package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VehicleMeterResetJpaRepository extends JpaRepository<VehicleMeterResetEntity, UUID> {
    List<VehicleMeterResetEntity> findByVehicleIdOrderByEffectiveAtDesc(UUID vehicleId);

    List<VehicleMeterResetEntity> findByVehicleIdAndReadingTypeOrderByEffectiveAtDesc(UUID vehicleId, String readingType);

    @Query("""
            SELECT r FROM VehicleMeterResetEntity r
            WHERE r.vehicleId = :vehicleId AND r.readingType = :readingType
            ORDER BY r.effectiveAt DESC, r.toEpoch DESC
            LIMIT 1
            """)
    Optional<VehicleMeterResetEntity> findLatest(@Param("vehicleId") UUID vehicleId,
                                                @Param("readingType") String readingType);
}