package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fuel_price")
@Getter
@Setter
@NoArgsConstructor
class FuelPriceEntity {
    @Id
    private UUID id;
    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;
    @Column(name = "fuel_type", nullable = false, length = 40)
    private String fuelType;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
