package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface DriverJpaRepository extends JpaRepository<DriverEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select driver from DriverEntity driver where driver.id = :id")
    Optional<DriverEntity> findByIdForUpdate(@Param("id") UUID id);
}
