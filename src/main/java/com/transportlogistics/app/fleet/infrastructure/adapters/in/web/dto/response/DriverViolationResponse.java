package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverViolationResponse(
        UUID id,
        UUID driverId,
        UUID tripId,
        String violationType,
        String severity,
        OffsetDateTime violationDate,
        int penaltyPoints,
        BigDecimal fineAmount,
        String paymentStatus,
        OffsetDateTime paidAt,
        String paymentReference,
        String location,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
}
