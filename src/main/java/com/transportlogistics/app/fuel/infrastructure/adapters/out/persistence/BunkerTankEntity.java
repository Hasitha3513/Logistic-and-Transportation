package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.BunkerTankStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bunker_tank")
@Getter
@Setter
class BunkerTankEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id
    private UUID id;

    @Column(name = "fuel_station_id", nullable = false)
    private UUID fuelStationId;

    @Column(name = "tank_code", nullable = false, length = 32, unique = true)
    private String tankCode;

    @Column(name = "tank_name", nullable = false, length = 128)
    private String tankName;

    @Column(name = "fuel_type", nullable = false, length = 32)
    private String fuelType;

    @Column(name = "capacity_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal capacityLiters;

    @Column(name = "current_stock_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal currentStockLiters;

    @Column(name = "minimum_stock_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal minimumStockLiters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BunkerTankStatus status;

    @Column(name = "commissioned_at", nullable = false)
    private OffsetDateTime commissionedAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
