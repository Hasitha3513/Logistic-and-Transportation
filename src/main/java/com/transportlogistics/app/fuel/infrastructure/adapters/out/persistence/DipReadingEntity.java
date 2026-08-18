package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bunker_dip_reading")
@Getter
@Setter
class DipReadingEntity {
    @Id
    private UUID id;

    @Column(name = "tank_id", nullable = false)
    private UUID tankId;

    @Column(name = "physical_quantity_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal physicalQuantityLiters;

    @Column(name = "book_quantity_at_measurement", nullable = false, precision = 12, scale = 3)
    private BigDecimal bookQuantityAtMeasurement;

    @Column(name = "variance_quantity_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal varianceQuantityLiters;

    @Column(name = "measured_at", nullable = false)
    private OffsetDateTime measuredAt;

    @Column(name = "measured_by", nullable = false)
    private UUID measuredBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
