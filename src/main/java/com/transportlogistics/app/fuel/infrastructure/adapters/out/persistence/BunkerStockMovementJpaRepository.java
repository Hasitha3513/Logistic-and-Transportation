package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.BunkerReferenceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface BunkerStockMovementJpaRepository extends JpaRepository<BunkerStockMovementEntity, UUID> {
    List<BunkerStockMovementEntity> findByTankIdOrderByOccurredAtDesc(UUID tankId, Pageable pageable);
    long countByTankId(UUID tankId);
    boolean existsByTankIdAndReferenceTypeAndReferenceId(UUID tankId, BunkerReferenceType referenceType, UUID referenceId);
}
