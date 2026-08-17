package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.FuelPurchaseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="fuel_purchase_history") @Getter @Setter @NoArgsConstructor
class FuelPurchaseHistoryEntity {
    @Id UUID id;
    @Column(name="fuel_purchase_id", nullable=false) UUID fuelPurchaseId;
    @Enumerated(EnumType.STRING) @Column(name="from_status") FuelPurchaseStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name="to_status", nullable=false) FuelPurchaseStatus toStatus;
    @Column(nullable=false, length=40) String action;
    @Column(name="actor_id", nullable=false) UUID actorId;
    @Column(nullable=false, length=80) String actor;
    @Column(length=1000) String comment;
    @Column(name="quantity_variance", precision=19, scale=4) BigDecimal quantityVariance;
    @Column(name="price_variance", precision=19, scale=2) BigDecimal priceVariance;
    @Column(name="occurred_at", nullable=false) OffsetDateTime occurredAt;
}
