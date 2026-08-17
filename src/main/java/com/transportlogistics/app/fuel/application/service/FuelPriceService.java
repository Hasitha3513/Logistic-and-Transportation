package com.transportlogistics.app.fuel.application.service;

import com.transportlogistics.app.fuel.application.ports.in.FuelPriceUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelVendorPort;
import com.transportlogistics.app.fuel.domain.model.FuelPrice;
import com.transportlogistics.app.fuel.domain.service.FuelPurchasePolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FuelPriceService implements FuelPriceUseCase {
    private final FuelPriceRepository prices;
    private final FuelVendorPort vendors;
    private final FuelPurchasePolicy policy;
    private final Clock clock;

    public FuelPriceService(FuelPriceRepository prices, FuelVendorPort vendors, FuelPurchasePolicy policy, Clock clock) {
        this.prices = prices;
        this.vendors = vendors;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public FuelPrice create(Command command) {
        return save(UUID.randomUUID(), command, null, OffsetDateTime.now(clock));
    }

    @Override
    public FuelPrice update(UUID id, Command command) {
        FuelPrice current = get(id);
        return save(id, command, id, current.createdAt());
    }

    private FuelPrice save(UUID id, Command command, UUID excludingId, OffsetDateTime createdAt) {
        requireActiveVendor(command.vendorId());
        String fuelType = normalize(command.fuelType());
        String currency = normalizeCurrency(command.currencyCode());
        var now = OffsetDateTime.now(clock);
        var price = new FuelPrice(id, command.vendorId(), fuelType, command.effectiveFrom(), command.effectiveTo(),
                command.unitPrice(), currency, command.active() == null || command.active(), createdAt, now);
        policy.validatePrice(price);
        if (price.active() && prices.hasOverlappingActivePrice(price.vendorId(), price.fuelType(),
                price.effectiveFrom(), price.effectiveTo(), excludingId)) {
            throw new ConflictException("FUEL_PRICE_OVERLAP", "An active fuel price already overlaps this period");
        }
        return prices.save(price);
    }

    @Override
    public FuelPrice get(UUID id) {
        return prices.findById(id).orElseThrow(() -> new NotFoundException("FUEL_PRICE_NOT_FOUND", "Fuel price not found: " + id));
    }

    @Override
    public List<FuelPrice> list(UUID vendorId, String fuelType, Boolean active, LocalDate effectiveOn) {
        return prices.find(vendorId, normalizeNullable(fuelType), active, effectiveOn);
    }

    private void requireActiveVendor(UUID vendorId) {
        var vendor = vendors.find(vendorId).orElseThrow(() -> new BusinessRuleException("FUEL_VENDOR_NOT_FOUND", "Vendor not found: " + vendorId));
        if (!vendor.active())
            throw new BusinessRuleException("FUEL_VENDOR_INACTIVE", "Inactive vendor cannot be used for fuel pricing");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank())
            throw new BusinessRuleException("FUEL_TYPE_REQUIRED", "Fuel type is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String value) {
        if (value == null || !value.trim().matches("[A-Za-z]{3}"))
            throw new BusinessRuleException("FUEL_CURRENCY_REQUIRED", "Currency must be a three-letter code");
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
