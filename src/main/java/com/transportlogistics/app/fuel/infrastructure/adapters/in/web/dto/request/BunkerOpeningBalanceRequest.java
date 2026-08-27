package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BunkerOpeningBalanceRequest(
        @NotNull @DecimalMin("0.001") BigDecimal openingBalanceLiters,
        String reason
) {}
