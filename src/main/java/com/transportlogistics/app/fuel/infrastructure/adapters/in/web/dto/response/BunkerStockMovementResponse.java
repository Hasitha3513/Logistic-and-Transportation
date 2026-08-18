package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.domain.model.BunkerMovementType;
import com.transportlogistics.app.fuel.domain.model.BunkerReferenceType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BunkerStockMovementResponse(
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
) {}
