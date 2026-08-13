package com.transportlogistics.app.organization.application.service;

import com.transportlogistics.app.organization.application.ports.in.LocationUseCase;
import com.transportlogistics.app.organization.application.ports.out.LocationRepository;
import com.transportlogistics.app.organization.domain.model.Location;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class LocationService implements LocationUseCase {
    private final LocationRepository repo;

    public LocationService(LocationRepository repo) {
        this.repo = repo;
    }

    public Location create(Location value) {
        return repo.save(value);
    }

    public Location get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Location not found: " + id));
    }

    public List<Location> list() {
        return repo.findAll();
    }

    public Location update(UUID id, Location value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Location(v.id(), v.code(), v.name(), v.address(), v.latitude(), v.longitude(), false));
    }
}
