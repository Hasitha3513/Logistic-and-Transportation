package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DriverMedicalRecordRequest(
        @NotNull(message = "Assessment date is required")
        LocalDate assessmentDate,

        @NotNull(message = "Valid from date is required")
        LocalDate validFrom,

        @NotNull(message = "Valid until date is required")
        LocalDate validUntil,

        @NotBlank(message = "Fitness status is required")
        String fitnessStatus,

        String visionTestStatus,
        String restrictions,
        String examinerOrProvider,
        String certificateReference,
        String remarks
) {}
