package com.transportlogistics.app.fleet.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverLicense(UUID id, UUID driverId, String licenseNumber, String licenseClass,
                            LocalDate issueDate, LocalDate expiryDate, DriverLicenseStatus status, boolean active,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt, String createdBy, String updatedBy) {
    public DriverLicense {
        if (id == null) throw new IllegalArgumentException("License id is required");
        if (driverId == null) throw new IllegalArgumentException("Driver id is required");
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new IllegalArgumentException("License number is required");
        }
        if (licenseClass == null || licenseClass.isBlank()) {
            throw new IllegalArgumentException("License class is required");
        }
        if (issueDate == null) throw new IllegalArgumentException("Issue date is required");
        if (expiryDate == null) throw new IllegalArgumentException("Expiry date is required");
        if (!expiryDate.isAfter(issueDate)) {
            throw new IllegalArgumentException("Expiry date must be later than issue date");
        }
        if (status == null) throw new IllegalArgumentException("License status is required");
        if (active != (status == DriverLicenseStatus.ACTIVE)) {
            throw new IllegalArgumentException("Active flag must match license status");
        }
        licenseNumber = licenseNumber.trim().toUpperCase();
        licenseClass = licenseClass.trim().toUpperCase();
    }

    public boolean isExpiredOn(LocalDate date) {
        return active && expiryDate.isBefore(date);
    }

    public boolean isValidFor(String requiredClass, LocalDate date) {
        return active && !isExpiredOn(date)
                && (requiredClass == null || requiredClass.isBlank()
                || licenseClass.equalsIgnoreCase(requiredClass.trim()));
    }
}
