package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockAdjustment(
        UUID id,
        UUID tankId,
        BigDecimal quantityDeltaLiters,
        String reason,
        UUID approvedBy,
        UUID sourceDipReadingId,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt
) {
}
