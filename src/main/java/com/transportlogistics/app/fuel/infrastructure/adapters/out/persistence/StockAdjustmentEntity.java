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
@Table(name = "bunker_stock_adjustment")
@Getter
@Setter
class StockAdjustmentEntity {
    @Id
    private UUID id;

    @Column(name = "tank_id", nullable = false)
    private UUID tankId;

    @Column(name = "quantity_delta_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityDeltaLiters;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "approved_by", nullable = false)
    private UUID approvedBy;

    @Column(name = "source_dip_reading_id")
    private UUID sourceDipReadingId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
