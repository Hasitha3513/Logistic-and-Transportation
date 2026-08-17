package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface VehicleMeterResetJpaRepository extends JpaRepository<VehicleMeterResetEntity, UUID> {
    List<VehicleMeterResetEntity> findByVehicleIdOrderByEffectiveAtDescCreatedAtDesc(UUID vehicleId);

    @Query("""
            select reset from VehicleMeterResetEntity reset
            where reset.vehicleId = :vehicleId
              and reset.readingType = :readingType
            order by reset.effectiveAt desc, reset.createdAt desc
            """)
    List<VehicleMeterResetEntity> findLatestByVehicleIdAndReadingType(@Param("vehicleId") UUID vehicleId,
                                                                      @Param("readingType") VehicleReadingType readingType,
                                                                      Pageable pageable);
}
