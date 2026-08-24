package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

interface VehicleJpaRepository extends JpaRepository<VehicleEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select vehicle from VehicleEntity vehicle where vehicle.id = :id")
    Optional<VehicleEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<VehicleEntity> findByRegistrationNumberIgnoreCase(String registrationNumber);

    Optional<VehicleEntity> findByChassisNumberIgnoreCase(String chassisNumber);

    Optional<VehicleEntity> findByEngineNumberIgnoreCase(String engineNumber);

    boolean existsByRegistrationNumberIgnoreCaseAndIdNot(String registrationNumber, UUID id);

    boolean existsByChassisNumberIgnoreCaseAndIdNot(String chassisNumber, UUID id);

    boolean existsByEngineNumberIgnoreCaseAndIdNot(String engineNumber, UUID id);
}
