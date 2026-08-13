package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.VehicleCategoryRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleCategory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class VehicleCategoryPersistenceAdapter implements VehicleCategoryRepository {
    private final VehicleCategoryJpaRepository repo;

    VehicleCategoryPersistenceAdapter(VehicleCategoryJpaRepository repo) {
        this.repo = repo;
    }

    public VehicleCategory save(VehicleCategory v) {
        var e = new VehicleCategoryEntity();
        e.setId(v.id());
        e.setCode(v.code());
        e.setName(v.name());
        e.setDescription(v.description());
        e.setActive(v.active());
        return map(repo.save(e));
    }

    public Optional<VehicleCategory> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public List<VehicleCategory> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    private VehicleCategory map(VehicleCategoryEntity e) {
        return new VehicleCategory(e.getId(), e.getCode(), e.getName(), e.getDescription(), e.isActive());
    }
}
