package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LubricantLogResponse(
        UUID id,
        UUID vehicleId,
        String fluidType,
        BigDecimal quantity,
        String unit,
        OffsetDateTime recordedAt,
        Double odometerKm,
        Double engineHours,
        UUID vendorId,
        String supplierName,
        String referenceNumber,
        String remarks,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
