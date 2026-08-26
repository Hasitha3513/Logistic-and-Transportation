package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BunkerStockMovement(
        UUID id,
        UUID tankId,
        BunkerMovementType movementType,
        BigDecimal quantityLiters,
        BigDecimal resultingBalanceLiters,
        BunkerReferenceType referenceType,
        UUID referenceId,
        OffsetDateTime occurredAt,
        UUID createdBy,
        String reason,
        OffsetDateTime createdAt
) {
}
