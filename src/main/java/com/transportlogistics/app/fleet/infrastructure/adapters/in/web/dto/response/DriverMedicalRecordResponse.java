package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverMedicalRecordResponse(
        UUID id,
        UUID driverId,
        LocalDate assessmentDate,
        LocalDate validFrom,
        LocalDate validUntil,
        String fitnessStatus,
        String visionTestStatus,
        String restrictions,
        String examinerOrProvider,
        String certificateReference,
        String remarks,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
