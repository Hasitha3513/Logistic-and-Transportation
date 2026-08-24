package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface FuelIssueHistoryJpaRepository extends JpaRepository<FuelIssueHistoryEntity, UUID> {
    List<FuelIssueHistoryEntity> findByFuelIssueIdOrderByOccurredAtAsc(UUID fuelIssueId);
}
