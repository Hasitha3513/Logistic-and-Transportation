package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverDrugTestResponse(
        UUID id,
        UUID driverId,
        String testType,
        LocalDate scheduledDate,
        OffsetDateTime sampleCollectedAt,
        LocalDate resultDate,
        String result,
        String status,
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
) {}
