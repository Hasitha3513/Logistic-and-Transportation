package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPriceResponse(UUID id,
                                UUID vendorId,
                                String fuelType,
                                LocalDate effectiveFrom,
                                LocalDate effectiveTo,
                                BigDecimal unitPrice,
                                String currencyCode,
                                boolean active,
                                OffsetDateTime createdAt,
                                OffsetDateTime updatedAt) {
}
