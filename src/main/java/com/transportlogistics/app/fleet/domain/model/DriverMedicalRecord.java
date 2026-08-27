package com.transportlogistics.app.fleet.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record DriverMedicalRecord(
        UUID id,
        UUID driverId,
        LocalDate assessmentDate,
        LocalDate validFrom,
        LocalDate validUntil,
        DriverMedicalStatus fitnessStatus,
        VisionTestStatus visionTestStatus,
        String restrictions,
        String examinerOrProvider,
        String certificateReference,
        String remarks,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public DriverMedicalRecord {
        Objects.requireNonNull(id, "Medical record ID cannot be null");
        Objects.requireNonNull(driverId, "Driver ID cannot be null");
        Objects.requireNonNull(assessmentDate, "Assessment date cannot be null");
        Objects.requireNonNull(validFrom, "Valid from date cannot be null");
        Objects.requireNonNull(validUntil, "Valid until date cannot be null");
        Objects.requireNonNull(fitnessStatus, "Fitness status cannot be null");
        if (validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException("Valid until date cannot precede valid from date");
        }
    }

    public boolean isFit() {
        return fitnessStatus == DriverMedicalStatus.FIT || fitnessStatus == DriverMedicalStatus.FIT_WITH_RESTRICTIONS;
    }

    public boolean isUnfit() {
        return fitnessStatus == DriverMedicalStatus.UNFIT || fitnessStatus == DriverMedicalStatus.TEMPORARILY_UNFIT;
    }

    public boolean isValidForPeriod(LocalDate from, LocalDate to) {
        if (!isFit() || !active) {
            return false;
        }
        return !validFrom.isAfter(from) && !validUntil.isBefore(to);
    }
}
