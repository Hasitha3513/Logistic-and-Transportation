package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DipReadingRequest(
        @NotNull @DecimalMin("0.000") BigDecimal physicalQuantityLiters,
        String notes
) {}
