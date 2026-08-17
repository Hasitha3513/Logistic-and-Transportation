package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "fuel_price") @Getter @Setter @NoArgsConstructor
class FuelPriceEntity {
    @Id UUID id;
    @Column(name = "vendor_id", nullable = false) UUID vendorId;
    @Column(name = "fuel_type", nullable = false, length = 40) String fuelType;
    @Column(name = "effective_from", nullable = false) LocalDate effectiveFrom;
    @Column(name = "effective_to") LocalDate effectiveTo;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4) BigDecimal unitPrice;
    @Column(name = "currency_code", nullable = false, length = 3) String currencyCode;
    @Column(nullable = false) boolean active;
    @Column(name = "created_at", nullable = false) OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) OffsetDateTime updatedAt;
}
