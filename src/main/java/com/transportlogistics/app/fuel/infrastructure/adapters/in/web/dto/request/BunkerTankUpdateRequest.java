package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fuel.domain.model.BunkerTankStatus;
import java.math.BigDecimal;

public record BunkerTankUpdateRequest(
        String tankName,
        BigDecimal minimumStockLiters,
        BunkerTankStatus status,
        Boolean active
) {}
