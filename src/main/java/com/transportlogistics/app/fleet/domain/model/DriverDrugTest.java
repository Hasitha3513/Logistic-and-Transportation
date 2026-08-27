package com.transportlogistics.app.fleet.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record DriverDrugTest(
        UUID id,
        UUID driverId,
        DrugTestType testType,
        LocalDate scheduledDate,
        OffsetDateTime sampleCollectedAt,
        LocalDate resultDate,
        DrugTestResult result,
        DrugTestStatus status,
        String laboratoryOrProvider,
        String referenceNumber,
        String remarks,
        boolean returnToDutyRequired,
        OffsetDateTime returnToDutyClearedAt,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
    public DriverDrugTest {
        Objects.requireNonNull(id, "Drug test ID cannot be null");
        Objects.requireNonNull(driverId, "Driver ID cannot be null");
        Objects.requireNonNull(testType, "Test type cannot be null");
        Objects.requireNonNull(scheduledDate, "Scheduled date cannot be null");
        Objects.requireNonNull(result, "Result cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
    }

    public boolean isBlocking() {
        if (!active) return false;
        if (result == DrugTestResult.POSITIVE) {
            return !returnToDutyRequired || returnToDutyClearedAt == null;
        }
        return false;
    }
}
