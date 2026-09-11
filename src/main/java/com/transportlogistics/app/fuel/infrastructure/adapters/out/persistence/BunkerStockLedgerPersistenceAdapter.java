package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.BunkerStockLedgerRepository;
import com.transportlogistics.app.fuel.domain.model.BunkerStockMovement;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class BunkerStockLedgerPersistenceAdapter implements BunkerStockLedgerRepository {

    private final BunkerStockMovementJpaRepository repository;

    BunkerStockLedgerPersistenceAdapter(BunkerStockMovementJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public BunkerStockMovement save(BunkerStockMovement m) {
        var entity = new BunkerStockMovementEntity();
        entity.setId(m.id());
        entity.setTankId(m.tankId());
        entity.setLedgerSequence(m.ledgerSequence());
        entity.setMovementType(m.movementType());
        entity.setQuantityLiters(m.quantityLiters());
        entity.setResultingBalanceLiters(m.resultingBalanceLiters());
        entity.setReferenceType(m.referenceType());
        entity.setReferenceId(m.referenceId());
        entity.setOccurredAt(m.occurredAt());
        entity.setCreatedBy(m.createdBy());
        entity.setReason(m.reason());
        entity.setCreatedAt(m.createdAt());
        return map(repository.save(entity));
    }

    @Override
    public long nextLedgerSequence(UUID tankId) {
        return repository.findMaxLedgerSequence(tankId) + 1L;
    }

    @Override
    public Optional<BunkerStockMovement> findById(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public List<BunkerStockMovement> findByTankIdPaged(UUID tankId, int offset, int limit) {
        int pageIndex = limit > 0 ? offset / limit : 0;
        int pageSize = limit > 0 ? limit : 20;
        return repository.findByTankIdOrderByLedgerSequenceDesc(tankId, PageRequest.of(pageIndex, pageSize))
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public long countByTankId(UUID tankId) {
        return repository.countByTankId(tankId);
    }

    @Override
    public boolean existsByTankIdAndReference(UUID tankId, com.transportlogistics.app.fuel.domain.model.BunkerReferenceType referenceType, UUID referenceId) {
        return repository.existsByTankIdAndReferenceTypeAndReferenceId(tankId, referenceType, referenceId);
    }

    private BunkerStockMovement map(BunkerStockMovementEntity e) {
        return new BunkerStockMovement(
                e.getId(),
                e.getTankId(),
                e.getLedgerSequence(),
                e.getMovementType(),
                e.getQuantityLiters(),
                e.getResultingBalanceLiters(),
                e.getReferenceType(),
                e.getReferenceId(),
                e.getOccurredAt(),
                e.getCreatedBy(),
                e.getReason(),
                e.getCreatedAt()
        );
    }
}
