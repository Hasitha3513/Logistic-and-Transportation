package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.VehicleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class VehicleService implements VehicleUseCase {
    private final VehicleRepository repo;

    public VehicleService(VehicleRepository repo) {
        this.repo = repo;
    }

    public Vehicle create(Vehicle value) {
        return repo.save(value);
    }

    public Vehicle get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));
    }

    public List<Vehicle> list() {
        return repo.findAll();
    }

    public Vehicle update(UUID id, Vehicle value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Vehicle(v.id(), v.registrationNumber(), v.chassisNumber(), v.engineNumber(), v.categoryId(), v.typeId(), v.manufacturer(), v.model(), v.manufactureYear(), v.ownershipType(), v.operationalStatus(), v.currentOdometerKm(), v.engineHours(), v.capacityKg(), false));
    }

}
