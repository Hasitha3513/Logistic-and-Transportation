package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.VehicleCategoryUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleCategoryRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleCategory;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class VehicleCategoryService implements VehicleCategoryUseCase {
    private final VehicleCategoryRepository repo;

    public VehicleCategoryService(VehicleCategoryRepository repo) {
        this.repo = repo;
    }

    public VehicleCategory create(VehicleCategory value) {
        return repo.save(value);
    }

    public VehicleCategory get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("VehicleCategory not found: " + id));
    }

    public List<VehicleCategory> list() {
        return repo.findAll();
    }

    public VehicleCategory update(UUID id, VehicleCategory value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new VehicleCategory(v.id(), v.code(), v.name(), v.description(), false));
    }
}
