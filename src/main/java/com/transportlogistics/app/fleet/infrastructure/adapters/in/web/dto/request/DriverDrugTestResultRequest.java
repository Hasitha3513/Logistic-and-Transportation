package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record DriverDrugTestResultRequest(
        @NotBlank(message = "Result is required")
        String result,

        LocalDate resultDate,
        String remarks,
        Boolean returnToDutyRequired
) {}
