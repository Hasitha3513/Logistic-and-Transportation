package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.DriverLicense;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface DriverLicenseRepository {
    DriverLicense save(DriverLicense license);

    Optional<DriverLicense> findById(UUID id);

    List<DriverLicense> findVisibleByDriverId(UUID driverId);

    List<DriverLicense> findActiveByDriverId(UUID driverId);

    List<DriverLicense> findActiveExpiringBy(LocalDate cutoffInclusive);

    boolean licenseNumberExists(String licenseNumber, UUID excludedId);
}
