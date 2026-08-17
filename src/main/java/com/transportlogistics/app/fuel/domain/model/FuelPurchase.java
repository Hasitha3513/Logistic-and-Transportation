package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FuelPurchase(
        UUID id, String purchaseNumber, UUID vendorId, UUID fuelStationId, String fuelType,
        LocalDate purchaseDate, String invoiceNumber, LocalDate invoiceDate,
        BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal, BigDecimal taxRate,
        BigDecimal taxAmount, BigDecimal otherCharges, BigDecimal totalAmount, String currencyCode,
        FuelPurchaseStatus status, ReconciliationStatus reconciliationStatus,
        BigDecimal receivedQuantity, BigDecimal quantityVariance, BigDecimal expectedUnitPrice,
        BigDecimal priceVariance, UUID destinationFuelStationId, String deliveryNoteNumber,
        OffsetDateTime receivedAt, UUID approvedBy, OffsetDateTime approvedAt,
        UUID reconciledBy, OffsetDateTime reconciledAt, String reconciliationNotes,
        String reconciliationReference, String notes, UUID createdBy, OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
