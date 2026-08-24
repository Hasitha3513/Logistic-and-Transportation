package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class DriverService implements DriverUseCase {
    private final DriverRepository repo;

    public DriverService(DriverRepository repo) {
        this.repo = repo;
    }

    public Driver create(Driver value) {
        return repo.save(value);
    }

    public Driver get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Driver not found: " + id));
    }

    public List<Driver> list() {
        return repo.findAll();
    }

    public Driver update(UUID id, Driver value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Driver(v.id(), v.employeeNumber(), v.firstName(), v.lastName(), v.phone(), v.email(), v.status(), false));
    }

}
