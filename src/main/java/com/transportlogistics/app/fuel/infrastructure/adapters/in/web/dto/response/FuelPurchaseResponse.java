package com.transportlogistics.app.fuel.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.domain.model.FuelPurchaseStatus;
import com.transportlogistics.app.fuel.domain.model.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPurchaseResponse(UUID id,
                                   String purchaseNumber,
                                   FuelPurchaseUseCase.VendorReference vendor,
                                   UUID fuelStationId,
                                   String fuelType,
                                   LocalDate purchaseDate,
                                   String invoiceNumber,
                                   LocalDate invoiceDate,
                                   BigDecimal quantity,
                                   BigDecimal unitPrice,
                                   BigDecimal subtotal,
                                   BigDecimal taxRate,
                                   BigDecimal taxAmount,
                                   BigDecimal otherCharges,
                                   BigDecimal totalAmount,
                                   String currencyCode,
                                   FuelPurchaseStatus status,
                                   ReconciliationStatus reconciliationStatus,
                                   BigDecimal receivedQuantity,
                                   BigDecimal quantityVariance,
                                   BigDecimal expectedUnitPrice,
                                   BigDecimal priceVariance,
                                   UUID destinationFuelStationId,
                                   String deliveryNoteNumber,
                                   OffsetDateTime receivedAt,
                                   UUID approvedBy,
                                   OffsetDateTime approvedAt,
                                   UUID reconciledBy,
                                   OffsetDateTime reconciledAt,
                                   String reconciliationNotes,
                                   String reconciliationReference,
                                   String notes,
                                   UUID createdBy,
                                   OffsetDateTime createdAt,
                                   OffsetDateTime updatedAt) {
}
