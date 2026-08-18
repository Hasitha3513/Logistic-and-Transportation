package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.domain.model.BunkerTankStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BunkerTankBalanceResponse(
        UUID tankId,
        UUID fuelStationId,
        String tankCode,
        String tankName,
        String fuelType,
        BigDecimal capacityLiters,
        BigDecimal currentStockLiters,
        BigDecimal availableCapacityLiters,
        BigDecimal minimumStockLiters,
        BunkerTankStatus status,
        String stockStatus,
        BigDecimal latestDipQuantityLiters,
        OffsetDateTime latestDipAt,
        BigDecimal latestVarianceLiters
) {}
