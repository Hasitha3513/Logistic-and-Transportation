package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.StockAdjustmentRepository;
import com.transportlogistics.app.fuel.domain.model.StockAdjustment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class StockAdjustmentPersistenceAdapter implements StockAdjustmentRepository {

    private final StockAdjustmentJpaRepository repository;

    StockAdjustmentPersistenceAdapter(StockAdjustmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public StockAdjustment save(StockAdjustment a) {
        var entity = new StockAdjustmentEntity();
        entity.setId(a.id());
        entity.setTankId(a.tankId());
        entity.setQuantityDeltaLiters(a.quantityDeltaLiters());
        entity.setReason(a.reason());
        entity.setApprovedBy(a.approvedBy());
        entity.setSourceDipReadingId(a.sourceDipReadingId());
        entity.setOccurredAt(a.occurredAt());
        entity.setCreatedAt(a.createdAt());
        return map(repository.save(entity));
    }

    @Override
    public Optional<StockAdjustment> findById(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public List<StockAdjustment> findByTankId(UUID tankId) {
        return repository.findByTankIdOrderByOccurredAtDesc(tankId).stream().map(this::map).toList();
    }

    private StockAdjustment map(StockAdjustmentEntity e) {
        return new StockAdjustment(
                e.getId(),
                e.getTankId(),
                e.getQuantityDeltaLiters(),
                e.getReason(),
                e.getApprovedBy(),
                e.getSourceDipReadingId(),
                e.getOccurredAt(),
                e.getCreatedAt()
        );
    }
}
