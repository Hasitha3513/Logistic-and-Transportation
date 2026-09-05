package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.BunkerReferenceType;
import com.transportlogistics.app.fuel.domain.model.BunkerStockMovement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BunkerStockLedgerRepository {

    BunkerStockMovement save(BunkerStockMovement movement);

    long nextLedgerSequence(UUID tankId);

    Optional<BunkerStockMovement> findById(UUID id);

    List<BunkerStockMovement> findByTankIdPaged(UUID tankId, int offset, int limit);

    long countByTankId(UUID tankId);

    boolean existsByTankIdAndReference(UUID tankId, BunkerReferenceType referenceType, UUID referenceId);
}
