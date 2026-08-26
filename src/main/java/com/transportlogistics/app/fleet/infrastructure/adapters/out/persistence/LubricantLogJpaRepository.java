package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface LubricantLogJpaRepository extends JpaRepository<LubricantLogEntity, UUID> {

    List<LubricantLogEntity> findByVehicleIdOrderByRecordedAtDesc(UUID vehicleId);

    @Query("SELECT l FROM LubricantLogEntity l WHERE l.vehicleId = :vehicleId " +
           "AND (:fluidType IS NULL OR l.fluidType = :fluidType) " +
           "AND (CAST(:from AS string) IS NULL OR l.recordedAt >= :from) " +
           "AND (CAST(:to AS string) IS NULL OR l.recordedAt <= :to) " +
           "ORDER BY l.recordedAt DESC")
    List<LubricantLogEntity> findByVehicleIdWithFilters(
            @Param("vehicleId") UUID vehicleId,
            @Param("fluidType") String fluidType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
