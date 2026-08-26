package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.StockAdjustment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockAdjustmentRepository {

    StockAdjustment save(StockAdjustment adjustment);

    Optional<StockAdjustment> findById(UUID id);

    List<StockAdjustment> findByTankId(UUID tankId);
}
