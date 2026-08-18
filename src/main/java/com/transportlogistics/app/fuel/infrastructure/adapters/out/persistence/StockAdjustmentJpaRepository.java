package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface StockAdjustmentJpaRepository extends JpaRepository<StockAdjustmentEntity, UUID> {
    List<StockAdjustmentEntity> findByTankIdOrderByOccurredAtDesc(UUID tankId);
}
