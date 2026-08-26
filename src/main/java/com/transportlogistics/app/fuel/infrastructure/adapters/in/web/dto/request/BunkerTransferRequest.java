package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record BunkerTransferRequest(
        @NotNull UUID sourceTankId,
        @NotNull UUID destinationTankId,
        @NotNull @DecimalMin("0.001") BigDecimal quantityLiters,
        String reason
) {}
