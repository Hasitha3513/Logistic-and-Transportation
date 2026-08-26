package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DipReadingResponse(
        UUID id,
        UUID tankId,
        BigDecimal physicalQuantityLiters,
        BigDecimal bookQuantityAtMeasurement,
        BigDecimal varianceQuantityLiters,
        OffsetDateTime measuredAt,
        UUID measuredBy,
        String notes,
        OffsetDateTime createdAt
) {}
