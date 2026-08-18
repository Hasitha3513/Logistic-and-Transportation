package com.transportlogistics.app.fuel.domain.policy;

import com.transportlogistics.app.fuel.domain.model.BunkerTank;
import com.transportlogistics.app.fuel.domain.model.BunkerTankStatus;
import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BunkerTankPolicy {

    public static final int QUANTITY_SCALE = 3;

    public void requireActive(BunkerTank tank) {
        if (tank.status() != BunkerTankStatus.ACTIVE || !tank.active()) {
            throw new BusinessRuleException("BUNKER_TANK_NOT_ACTIVE", "Bunker tank is not active: " + tank.tankCode());
        }
    }

    public void validateFuelType(BunkerTank tank, String requestedFuelType) {
        if (requestedFuelType == null || !tank.fuelType().equalsIgnoreCase(requestedFuelType.trim())) {
            throw new BusinessRuleException("BUNKER_FUEL_TYPE_MISMATCH",
                    "Fuel type mismatch: tank requires " + tank.fuelType() + " but received " + requestedFuelType);
        }
    }

    public void validateReceivable(BunkerTank tank, BigDecimal quantity, String incomingFuelType) {
        requireActive(tank);
        validateFuelType(tank, incomingFuelType);
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("INVALID_BUNKER_QUANTITY", "Receipt quantity must be greater than zero");
        }
        BigDecimal newStock = tank.currentStockLiters().add(quantity).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        if (newStock.compareTo(tank.capacityLiters()) > 0) {
            throw new BusinessRuleException("BUNKER_CAPACITY_EXCEEDED",
                    "Receipt quantity " + quantity + " L exceeds tank remaining capacity of " + tank.availableCapacity() + " L");
        }
    }

    public void validateIssuable(BunkerTank tank, BigDecimal quantity, String requestedFuelType) {
        requireActive(tank);
        validateFuelType(tank, requestedFuelType);
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("INVALID_BUNKER_QUANTITY", "Issue quantity must be greater than zero");
        }
        if (tank.currentStockLiters().compareTo(quantity) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_BUNKER_STOCK",
                    "Insufficient stock: requested " + quantity + " L but available stock is " + tank.currentStockLiters() + " L");
        }
    }

    public void validateTransfer(BunkerTank source, BunkerTank destination, BigDecimal quantity) {
        requireActive(source);
        requireActive(destination);
        if (source.id().equals(destination.id())) {
            throw new BusinessRuleException("INVALID_BUNKER_TRANSFER", "Source and destination tanks cannot be the same");
        }
        validateFuelType(source, destination.fuelType());
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("INVALID_BUNKER_QUANTITY", "Transfer quantity must be greater than zero");
        }
        if (source.currentStockLiters().compareTo(quantity) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_BUNKER_STOCK",
                    "Insufficient source stock: requested " + quantity + " L but available is " + source.currentStockLiters() + " L");
        }
        BigDecimal destNewStock = destination.currentStockLiters().add(quantity).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        if (destNewStock.compareTo(destination.capacityLiters()) > 0) {
            throw new BusinessRuleException("BUNKER_CAPACITY_EXCEEDED",
                    "Transfer quantity " + quantity + " L exceeds destination tank remaining capacity of " + destination.availableCapacity() + " L");
        }
    }
}
