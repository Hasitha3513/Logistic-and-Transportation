package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReceiptRequest(@NotNull @DecimalMin("0.0001") BigDecimal receivedQuantity,
                             OffsetDateTime receivedAt,
                             UUID destinationFuelStationId,
                             @Size(max = 100) String deliveryNoteNumber,
                             @Size(max = 1000) String remarks) {

    public FuelPurchaseUseCase.ReceiptCommand command() {
        return new FuelPurchaseUseCase.ReceiptCommand(receivedQuantity, receivedAt, destinationFuelStationId,
                deliveryNoteNumber, remarks);
    }
}
