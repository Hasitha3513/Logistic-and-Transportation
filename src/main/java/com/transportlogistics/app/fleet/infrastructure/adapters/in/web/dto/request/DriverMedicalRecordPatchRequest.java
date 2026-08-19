package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import java.time.LocalDate;

public record DriverMedicalRecordPatchRequest(
        LocalDate assessmentDate,
        LocalDate validFrom,
        LocalDate validUntil,
        String fitnessStatus,
        String visionTestStatus,
        String restrictions,
        String examinerOrProvider,
        String certificateReference,
        String remarks,
        Boolean active
) {}
