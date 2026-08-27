package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FuelPurchaseRequest(@NotNull UUID vendorId,
                                  UUID fuelStationId,
                                  @NotBlank @Size(max = 40) String fuelType,
                                  @NotNull LocalDate purchaseDate,
                                  @Size(max = 100) String invoiceNumber,
                                  LocalDate invoiceDate,
                                  @NotNull @DecimalMin("0.0001") BigDecimal quantity,
                                  @NotNull @DecimalMin(value = "0.0001") BigDecimal unitPrice,
                                  @DecimalMin("0.0") BigDecimal taxRate,
                                  @DecimalMin("0.0") BigDecimal otherCharges,
                                  @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currencyCode,
                                  @Size(max = 1000) String notes) {

    public FuelPurchaseUseCase.Command command() {
        return new FuelPurchaseUseCase.Command(vendorId, fuelStationId, fuelType, purchaseDate, invoiceNumber,
                invoiceDate, quantity, unitPrice, taxRate, otherCharges, currencyCode, notes);
    }
}
