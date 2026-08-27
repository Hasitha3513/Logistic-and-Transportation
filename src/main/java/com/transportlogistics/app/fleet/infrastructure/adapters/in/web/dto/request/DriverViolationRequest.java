package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fleet.domain.model.DriverViolationType;
import com.transportlogistics.app.fleet.domain.model.ViolationSeverity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverViolationRequest(
        UUID tripId,
        @NotNull(message = "Violation type is required") DriverViolationType violationType,
        @NotNull(message = "Violation severity is required") ViolationSeverity severity,
        @NotNull(message = "Violation date is required") OffsetDateTime violationDate,
        @Min(value = 0, message = "Penalty points cannot be negative") Integer penaltyPoints,
        @Min(value = 0, message = "Fine amount cannot be negative") BigDecimal fineAmount,
        String location,
        String description
) {
}
