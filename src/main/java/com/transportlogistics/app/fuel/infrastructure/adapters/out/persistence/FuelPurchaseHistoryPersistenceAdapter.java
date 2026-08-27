package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseHistoryRepository;
import com.transportlogistics.app.fuel.domain.model.FuelPurchaseHistory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
class FuelPurchaseHistoryPersistenceAdapter implements FuelPurchaseHistoryRepository {
    private final FuelPurchaseHistoryJpaRepository repository;
    FuelPurchaseHistoryPersistenceAdapter(FuelPurchaseHistoryJpaRepository repository) { this.repository = repository; }
    @Override public FuelPurchaseHistory save(FuelPurchaseHistory h) { return map(repository.save(entity(h))); }
    @Override public List<FuelPurchaseHistory> findByPurchaseId(UUID id) { return repository.findByFuelPurchaseIdOrderByOccurredAtAsc(id).stream().map(this::map).toList(); }
    private FuelPurchaseHistoryEntity entity(FuelPurchaseHistory h) { var e = new FuelPurchaseHistoryEntity(); e.setId(h.id()); e.setFuelPurchaseId(h.fuelPurchaseId()); e.setFromStatus(h.fromStatus()); e.setToStatus(h.toStatus()); e.setAction(h.action()); e.setActorId(h.actorId()); e.setActor(h.actor()); e.setComment(h.comment()); e.setQuantityVariance(h.quantityVariance()); e.setPriceVariance(h.priceVariance()); e.setOccurredAt(h.occurredAt()); return e; }
    private FuelPurchaseHistory map(FuelPurchaseHistoryEntity e) { return new FuelPurchaseHistory(e.getId(), e.getFuelPurchaseId(), e.getFromStatus(), e.getToStatus(), e.getAction(), e.getActorId(), e.getActor(), e.getComment(), e.getQuantityVariance(), e.getPriceVariance(), e.getOccurredAt()); }
}
