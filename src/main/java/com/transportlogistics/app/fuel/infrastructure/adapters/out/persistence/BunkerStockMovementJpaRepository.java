package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.domain.model.BunkerReferenceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface BunkerStockMovementJpaRepository extends JpaRepository<BunkerStockMovementEntity, UUID> {
    List<BunkerStockMovementEntity> findByTankIdOrderByLedgerSequenceDesc(UUID tankId, Pageable pageable);
    @Query("select coalesce(max(m.ledgerSequence), 0) from BunkerStockMovementEntity m where m.tankId = :tankId")
    long findMaxLedgerSequence(@Param("tankId") UUID tankId);
    long countByTankId(UUID tankId);
    boolean existsByTankIdAndReferenceTypeAndReferenceId(UUID tankId, BunkerReferenceType referenceType, UUID referenceId);
}
