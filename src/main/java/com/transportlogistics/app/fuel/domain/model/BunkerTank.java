package com.transportlogistics.app.fuel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BunkerTank(
        UUID id,
        UUID fuelStationId,
        String tankCode,
        String tankName,
        String fuelType,
        BigDecimal capacityLiters,
        BigDecimal currentStockLiters,
        BigDecimal minimumStockLiters,
        BunkerTankStatus status,
        OffsetDateTime commissionedAt,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public BigDecimal availableCapacity() {
        if (capacityLiters == null || currentStockLiters == null) {
            return BigDecimal.ZERO;
        }
        return capacityLiters.subtract(currentStockLiters).max(BigDecimal.ZERO);
    }

    public boolean isLowStock() {
        if (currentStockLiters == null || minimumStockLiters == null) {
            return false;
        }
        return currentStockLiters.compareTo(minimumStockLiters) <= 0;
    }

    public BunkerTank withStock(BigDecimal newStock) {
        return new BunkerTank(id, fuelStationId, tankCode, tankName, fuelType, capacityLiters, newStock,
                minimumStockLiters, status, commissionedAt, active, createdAt, OffsetDateTime.now());
    }

    public BunkerTank withStatus(BunkerTankStatus newStatus) {
        return new BunkerTank(id, fuelStationId, tankCode, tankName, fuelType, capacityLiters, currentStockLiters,
                minimumStockLiters, newStatus, commissionedAt, active, createdAt, OffsetDateTime.now());
    }
}
