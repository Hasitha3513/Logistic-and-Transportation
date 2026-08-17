package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface FuelPurchaseHistoryJpaRepository extends JpaRepository<FuelPurchaseHistoryEntity, UUID> {
    List<FuelPurchaseHistoryEntity> findByFuelPurchaseIdOrderByOccurredAtAsc(UUID id);
}
