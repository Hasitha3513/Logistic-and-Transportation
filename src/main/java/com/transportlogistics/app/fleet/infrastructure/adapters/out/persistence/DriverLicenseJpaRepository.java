package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

interface DriverLicenseJpaRepository extends JpaRepository<DriverLicenseEntity, UUID> {
    List<DriverLicenseEntity> findByDriverIdAndStatusNotOrderByCreatedAtDesc(UUID driverId,
                                                                            DriverLicenseStatus status);

    List<DriverLicenseEntity> findByDriverIdAndActiveTrue(UUID driverId);

    List<DriverLicenseEntity> findByActiveTrueAndExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate cutoffInclusive);

    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, UUID id);
}
