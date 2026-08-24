package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.BunkerTankRepository;
import com.transportlogistics.app.fuel.domain.model.BunkerTank;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class BunkerTankPersistenceAdapter implements BunkerTankRepository {

    private final BunkerTankJpaRepository repository;
    private final jakarta.persistence.EntityManager entityManager;

    BunkerTankPersistenceAdapter(BunkerTankJpaRepository repository, jakarta.persistence.EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public BunkerTank save(BunkerTank tank) {
        var entity = new BunkerTankEntity();
        entity.setId(tank.id());
        entity.setFuelStationId(tank.fuelStationId());
        entity.setTankCode(tank.tankCode());
        entity.setTankName(tank.tankName());
        entity.setFuelType(tank.fuelType());
        entity.setCapacityLiters(tank.capacityLiters());
        entity.setCurrentStockLiters(tank.currentStockLiters());
        entity.setMinimumStockLiters(tank.minimumStockLiters());
        entity.setStatus(tank.status());
        entity.setCommissionedAt(tank.commissionedAt());
        entity.setActive(tank.active());
        entity.setCreatedAt(tank.createdAt());
        entity.setUpdatedAt(tank.updatedAt());
        return map(repository.save(entity));
    }

    @Override
    public Optional<BunkerTank> findById(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public Optional<BunkerTank> findByIdForUpdate(UUID id) {
        var opt = repository.findByIdForUpdate(id);
        opt.ifPresent(entityManager::refresh);
        return opt.map(this::map);
    }

    @Override
    public Optional<BunkerTank> findByTankCode(String tankCode) {
        return repository.findByTankCodeIgnoreCase(tankCode).map(this::map);
    }

    @Override
    public Optional<BunkerTank> findActiveByStationAndFuelType(UUID fuelStationId, String fuelType) {
        return repository.findActiveByStationAndFuelType(fuelStationId, fuelType).map(this::map);
    }

    @Override
    public Optional<BunkerTank> findActiveByStationAndFuelTypeForUpdate(UUID fuelStationId, String fuelType) {
        var opt = repository.findActiveByStationAndFuelTypeForUpdate(fuelStationId, fuelType);
        opt.ifPresent(entityManager::refresh);
        return opt.map(this::map);
    }

    @Override
    public List<BunkerTank> list(UUID fuelStationId, String fuelType, Boolean active) {
        Specification<BunkerTankEntity> spec = Specification.where(null);
        if (fuelStationId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("fuelStationId"), fuelStationId));
        }
        if (fuelType != null && !fuelType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("fuelType")), fuelType.trim().toUpperCase()));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        return repository.findAll(spec).stream().map(this::map).toList();
    }

    private BunkerTank map(BunkerTankEntity e) {
        return new BunkerTank(
                e.getId(),
                e.getFuelStationId(),
                e.getTankCode(),
                e.getTankName(),
                e.getFuelType(),
                e.getCapacityLiters(),
                e.getCurrentStockLiters(),
                e.getMinimumStockLiters(),
                e.getStatus(),
                e.getCommissionedAt(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
