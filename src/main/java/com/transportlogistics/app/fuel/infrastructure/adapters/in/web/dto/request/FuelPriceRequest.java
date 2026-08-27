package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fuel.application.ports.in.FuelPriceUseCase;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FuelPriceRequest(@NotNull UUID vendorId,
                               @NotBlank @Size(max = 40) String fuelType,
                               @NotNull LocalDate effectiveFrom,
                               LocalDate effectiveTo,
                               @NotNull @DecimalMin("0.0001") BigDecimal unitPrice,
                               @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currencyCode,
                               Boolean active) {

    public FuelPriceUseCase.Command command() {
        return new FuelPriceUseCase.Command(vendorId, fuelType, effectiveFrom, effectiveTo, unitPrice,
                currencyCode, active);
    }
}
