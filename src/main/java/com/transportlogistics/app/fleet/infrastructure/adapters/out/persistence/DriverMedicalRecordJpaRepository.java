package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

interface DriverMedicalRecordJpaRepository extends JpaRepository<DriverMedicalRecordEntity, UUID> {

    List<DriverMedicalRecordEntity> findByDriverIdOrderByValidUntilDesc(UUID driverId);

    @Query("SELECT r FROM DriverMedicalRecordEntity r WHERE r.driverId = :driverId AND r.active = true ORDER BY r.validUntil DESC, r.assessmentDate DESC LIMIT 1")
    Optional<DriverMedicalRecordEntity> findLatestApplicableByDriverId(@Param("driverId") UUID driverId);

    @Query("""
        SELECT r FROM DriverMedicalRecordEntity r
        WHERE r.active = true
          AND r.fitnessStatus IN ('FIT', 'FIT_WITH_RESTRICTIONS')
          AND r.validUntil <= :cutoff
        ORDER BY r.validUntil
        """)
    List<DriverMedicalRecordEntity> findActiveFitExpiringBy(@Param("cutoff") LocalDate cutoffInclusive);
}
