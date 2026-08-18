package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockAdjustmentResponse(
        UUID id,
        UUID tankId,
        BigDecimal quantityDeltaLiters,
        String reason,
        UUID approvedBy,
        UUID sourceDipReadingId,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt
) {}
