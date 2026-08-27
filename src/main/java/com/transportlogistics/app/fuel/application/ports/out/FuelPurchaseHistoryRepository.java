package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.FuelPurchaseHistory;

import java.util.List;
import java.util.UUID;

public interface FuelPurchaseHistoryRepository {
    FuelPurchaseHistory save(FuelPurchaseHistory history);
    List<FuelPurchaseHistory> findByPurchaseId(UUID fuelPurchaseId);
}
