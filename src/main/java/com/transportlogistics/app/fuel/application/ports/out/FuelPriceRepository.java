package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.FuelPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuelPriceRepository {
    FuelPrice save(FuelPrice price);
    Optional<FuelPrice> findById(UUID id);
    List<FuelPrice> find(UUID vendorId, String fuelType, Boolean active, LocalDate effectiveOn);
    boolean hasOverlappingActivePrice(UUID vendorId, String fuelType, LocalDate from, LocalDate to, UUID excludingId);
    Optional<FuelPrice> findEffective(UUID vendorId, String fuelType, LocalDate date);
}
