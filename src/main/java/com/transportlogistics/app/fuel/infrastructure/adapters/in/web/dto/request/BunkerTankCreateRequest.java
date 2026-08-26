package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BunkerTankCreateRequest(
        @NotNull UUID fuelStationId,
        @NotBlank String tankCode,
        @NotBlank String tankName,
        @NotBlank String fuelType,
        @NotNull @DecimalMin("0.001") BigDecimal capacityLiters,
        BigDecimal minimumStockLiters,
        BigDecimal openingBalanceLiters,
        OffsetDateTime commissionedAt
) {}
