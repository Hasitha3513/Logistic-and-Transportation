package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface BunkerTankJpaRepository extends JpaRepository<BunkerTankEntity, UUID>, JpaSpecificationExecutor<BunkerTankEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM BunkerTankEntity t WHERE t.id = :id")
    Optional<BunkerTankEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<BunkerTankEntity> findByTankCodeIgnoreCase(String tankCode);

    @Query("SELECT t FROM BunkerTankEntity t WHERE t.fuelStationId = :stationId AND UPPER(t.fuelType) = UPPER(:fuelType) AND t.active = true AND t.status = 'ACTIVE'")
    Optional<BunkerTankEntity> findActiveByStationAndFuelType(@Param("stationId") UUID stationId, @Param("fuelType") String fuelType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM BunkerTankEntity t WHERE t.fuelStationId = :stationId AND UPPER(t.fuelType) = UPPER(:fuelType) AND t.active = true AND t.status = 'ACTIVE'")
    Optional<BunkerTankEntity> findActiveByStationAndFuelTypeForUpdate(@Param("stationId") UUID stationId, @Param("fuelType") String fuelType);
}
