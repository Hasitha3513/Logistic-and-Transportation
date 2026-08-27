package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DriverLicenseUseCase {
    List<DriverLicense> list(UUID driverId);

    DriverLicense create(UUID driverId, CreateCommand command, String actor);

    DriverLicense update(UUID driverId, UUID licenseId, UpdateCommand command, String actor);

    void delete(UUID driverId, UUID licenseId, String actor);

    record CreateCommand(String licenseNumber, String licenseClass, LocalDate issueDate, LocalDate expiryDate,
                         DriverLicenseStatus status, Boolean active) {
    }

    record UpdateCommand(String licenseNumber, String licenseClass, LocalDate issueDate, LocalDate expiryDate,
                         DriverLicenseStatus status, Boolean active) {
    }
}
