package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DriverDrugTestJpaRepository extends JpaRepository<DriverDrugTestEntity, UUID> {

    List<DriverDrugTestEntity> findByDriverIdOrderByScheduledDateDescCreatedAtDesc(UUID driverId);

    List<DriverDrugTestEntity> findByDriverIdAndActiveTrueOrderByScheduledDateDesc(UUID driverId);

    @Query("SELECT t FROM DriverDrugTestEntity t WHERE t.driverId = :driverId AND t.active = true ORDER BY t.scheduledDate DESC, t.createdAt DESC LIMIT 1")
    Optional<DriverDrugTestEntity> findLatestByDriverId(@Param("driverId") UUID driverId);
}
