package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.BunkerMovementType;
import com.transportlogistics.app.fuel.domain.model.BunkerReferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bunker_stock_movement")
@Getter
@Setter
class BunkerStockMovementEntity {
    @Id
    private UUID id;

    @Column(name = "tank_id", nullable = false)
    private UUID tankId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 32)
    private BunkerMovementType movementType;

    @Column(name = "quantity_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityLiters;

    @Column(name = "resulting_balance_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal resultingBalanceLiters;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 32)
    private BunkerReferenceType referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
