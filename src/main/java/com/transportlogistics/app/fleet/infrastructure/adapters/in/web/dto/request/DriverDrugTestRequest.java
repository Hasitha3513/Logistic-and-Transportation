package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DriverDrugTestRequest(
        @NotBlank(message = "Test type is required")
        String testType,

        @NotNull(message = "Scheduled date is required")
        LocalDate scheduledDate,

        String laboratoryOrProvider,
        String referenceNumber,
        String remarks
) {}
