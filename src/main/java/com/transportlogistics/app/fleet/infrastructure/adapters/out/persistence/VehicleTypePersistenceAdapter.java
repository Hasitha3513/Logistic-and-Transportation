package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.VehicleTypeRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class VehicleTypePersistenceAdapter implements VehicleTypeRepository {
    private final VehicleTypeJpaRepository repo;

    VehicleTypePersistenceAdapter(VehicleTypeJpaRepository repo) {
        this.repo = repo;
    }

    public VehicleType save(VehicleType v) {
        var e = new VehicleTypeEntity();
        e.setId(v.id());
        e.setCategoryId(v.categoryId());
        e.setCode(v.code());
        e.setName(v.name());
        e.setDescription(v.description());
        e.setActive(v.active());
        return map(repo.save(e));
    }

    public Optional<VehicleType> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public List<VehicleType> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    private VehicleType map(VehicleTypeEntity e) {
        return new VehicleType(e.getId(), e.getCategoryId(), e.getCode(), e.getName(), e.getDescription(), e.isActive());
    }
}
