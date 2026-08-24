package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DipReading(
        UUID id,
        UUID tankId,
        BigDecimal physicalQuantityLiters,
        BigDecimal bookQuantityAtMeasurement,
        BigDecimal varianceQuantityLiters,
        OffsetDateTime measuredAt,
        UUID measuredBy,
        String notes,
        OffsetDateTime createdAt
) {
}
