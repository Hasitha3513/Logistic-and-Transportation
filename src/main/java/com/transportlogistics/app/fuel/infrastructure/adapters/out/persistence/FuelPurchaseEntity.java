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

@Entity
@Table(name = "fuel_purchase")
@Getter
@Setter
@NoArgsConstructor
class FuelPurchaseEntity {
    @Id
    private UUID id;
    @Column(name = "purchase_number", nullable = false, unique = true, length = 60)
    private String purchaseNumber;
    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;
    @Column(name = "fuel_station_id")
    private UUID fuelStationId;
    @Column(name = "fuel_type", nullable = false, length = 40)
    private String fuelType;
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;
    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;
    @Column(name = "invoice_date")
    private LocalDate invoiceDate;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
    @Column(name = "tax_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxRate;
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;
    @Column(name = "other_charges", nullable = false, precision = 19, scale = 2)
    private BigDecimal otherCharges;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FuelPurchaseStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus;
    @Column(name = "received_quantity", precision = 19, scale = 4)
    private BigDecimal receivedQuantity;
    @Column(name = "quantity_variance", precision = 19, scale = 4)
    private BigDecimal quantityVariance;
    @Column(name = "expected_unit_price", precision = 19, scale = 4)
    private BigDecimal expectedUnitPrice;
    @Column(name = "price_variance", precision = 19, scale = 2)
    private BigDecimal priceVariance;
    @Column(name = "destination_fuel_station_id")
    private UUID destinationFuelStationId;
    @Column(name = "delivery_note_number", length = 100)
    private String deliveryNoteNumber;
    @Column(name = "received_at")
    private OffsetDateTime receivedAt;
    @Column(name = "approved_by")
    private UUID approvedBy;
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;
    @Column(name = "reconciled_by")
    private UUID reconciledBy;
    @Column(name = "reconciled_at")
    private OffsetDateTime reconciledAt;
    @Column(name = "reconciliation_notes", length = 1000)
    private String reconciliationNotes;
    @Column(name = "reconciliation_reference", length = 100)
    private String reconciliationReference;
    @Column(length = 1000)
    private String notes;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
