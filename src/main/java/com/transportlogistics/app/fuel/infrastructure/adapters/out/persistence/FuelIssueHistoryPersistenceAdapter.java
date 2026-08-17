package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.FuelIssueHistoryRepository;
import com.transportlogistics.app.fuel.domain.model.FuelIssueHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FuelIssueHistoryPersistenceAdapter implements FuelIssueHistoryRepository {
    private final FuelIssueHistoryJpaRepository repository;

    @Override
    public FuelIssueHistory save(FuelIssueHistory history) {
        var entity = new FuelIssueHistoryEntity();
        entity.setId(history.id());
        entity.setFuelIssueId(history.fuelIssueId());
        entity.setFromStatus(history.fromStatus());
        entity.setToStatus(history.toStatus());
        entity.setAction(history.action());
        entity.setActorId(history.actorId());
        entity.setActor(history.actor());
        entity.setComment(history.comment());
        entity.setOccurredAt(history.occurredAt());
        return map(repository.save(entity));
    }

    @Override
    public List<FuelIssueHistory> findByFuelIssueId(UUID fuelIssueId) {
        return repository.findByFuelIssueIdOrderByOccurredAtAsc(fuelIssueId).stream().map(this::map).toList();
    }

    private FuelIssueHistory map(FuelIssueHistoryEntity entity) {
        return new FuelIssueHistory(entity.getId(), entity.getFuelIssueId(), entity.getFromStatus(),
                entity.getToStatus(), entity.getAction(), entity.getActorId(), entity.getActor(),
                entity.getComment(), entity.getOccurredAt());
    }
}
