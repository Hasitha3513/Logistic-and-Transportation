package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.FuelPurchaseStatus;
import com.transportlogistics.app.fuel.domain.model.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "fuel_purchase") @Getter @Setter @NoArgsConstructor
class FuelPurchaseEntity {
    @Id UUID id;
    @Column(name="purchase_number", nullable=false, unique=true, length=60) String purchaseNumber;
    @Column(name="vendor_id", nullable=false) UUID vendorId;
    @Column(name="fuel_station_id") UUID fuelStationId;
    @Column(name="fuel_type", nullable=false, length=40) String fuelType;
    @Column(name="purchase_date", nullable=false) LocalDate purchaseDate;
    @Column(name="invoice_number", length=100) String invoiceNumber;
    @Column(name="invoice_date") LocalDate invoiceDate;
    @Column(nullable=false, precision=19, scale=4) BigDecimal quantity;
    @Column(name="unit_price", nullable=false, precision=19, scale=4) BigDecimal unitPrice;
    @Column(nullable=false, precision=19, scale=2) BigDecimal subtotal;
    @Column(name="tax_rate", nullable=false, precision=8, scale=4) BigDecimal taxRate;
    @Column(name="tax_amount", nullable=false, precision=19, scale=2) BigDecimal taxAmount;
    @Column(name="other_charges", nullable=false, precision=19, scale=2) BigDecimal otherCharges;
    @Column(name="total_amount", nullable=false, precision=19, scale=2) BigDecimal totalAmount;
    @Column(name="currency_code", nullable=false, length=3) String currencyCode;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) FuelPurchaseStatus status;
    @Enumerated(EnumType.STRING) @Column(name="reconciliation_status", nullable=false, length=30) ReconciliationStatus reconciliationStatus;
    @Column(name="received_quantity", precision=19, scale=4) BigDecimal receivedQuantity;
    @Column(name="quantity_variance", precision=19, scale=4) BigDecimal quantityVariance;
    @Column(name="expected_unit_price", precision=19, scale=4) BigDecimal expectedUnitPrice;
    @Column(name="price_variance", precision=19, scale=2) BigDecimal priceVariance;
    @Column(name="destination_fuel_station_id") UUID destinationFuelStationId;
    @Column(name="delivery_note_number", length=100) String deliveryNoteNumber;
    @Column(name="received_at") OffsetDateTime receivedAt;
    @Column(name="approved_by") UUID approvedBy;
    @Column(name="approved_at") OffsetDateTime approvedAt;
    @Column(name="reconciled_by") UUID reconciledBy;
    @Column(name="reconciled_at") OffsetDateTime reconciledAt;
    @Column(name="reconciliation_notes", length=1000) String reconciliationNotes;
    @Column(name="reconciliation_reference", length=100) String reconciliationReference;
    @Column(length=1000) String notes;
    @Column(name="created_by", nullable=false) UUID createdBy;
    @Column(name="created_at", nullable=false) OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false) OffsetDateTime updatedAt;
}
