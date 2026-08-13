package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.VehicleTypeUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleTypeRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleType;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class VehicleTypeService implements VehicleTypeUseCase {
    private final VehicleTypeRepository repo;

    public VehicleTypeService(VehicleTypeRepository repo) {
        this.repo = repo;
    }

    public VehicleType create(VehicleType value) {
        return repo.save(value);
    }

    public VehicleType get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("VehicleType not found: " + id));
    }

    public List<VehicleType> list() {
        return repo.findAll();
    }

    public VehicleType update(UUID id, VehicleType value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new VehicleType(v.id(), v.categoryId(), v.code(), v.name(), v.description(), false));
    }
}
