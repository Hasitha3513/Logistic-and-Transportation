package com.transportlogistics.app.fuel.application.ports.in;

import com.transportlogistics.app.fuel.domain.model.FuelPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FuelPriceUseCase {
    FuelPrice create(Command command);

    FuelPrice update(UUID id, Command command);

    FuelPrice get(UUID id);

    List<FuelPrice> list(UUID vendorId, String fuelType, Boolean active, LocalDate effectiveOn);

    record Command(UUID vendorId, String fuelType, LocalDate effectiveFrom, LocalDate effectiveTo,
                   BigDecimal unitPrice, String currencyCode, Boolean active) {
    }
}
