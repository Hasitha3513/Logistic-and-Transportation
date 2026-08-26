package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPrice(UUID id, UUID vendorId, String fuelType, LocalDate effectiveFrom, LocalDate effectiveTo,
                        BigDecimal unitPrice, String currencyCode, boolean active,
                        OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
