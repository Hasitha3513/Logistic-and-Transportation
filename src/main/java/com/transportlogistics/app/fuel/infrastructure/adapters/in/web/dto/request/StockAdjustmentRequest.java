package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull BigDecimal quantityDeltaLiters,
        @NotBlank String reason,
        UUID sourceDipReadingId
) {}
