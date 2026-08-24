package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class DriverPersistenceAdapter implements DriverRepository {
    private final DriverJpaRepository repo;

    DriverPersistenceAdapter(DriverJpaRepository repo) {
        this.repo = repo;
    }

    public Driver save(Driver v) {
        var e = new DriverEntity();
        e.setId(v.id());
        e.setEmployeeNumber(v.employeeNumber());
        e.setFirstName(v.firstName());
        e.setLastName(v.lastName());
        e.setPhone(v.phone());
        e.setEmail(v.email());
        e.setStatus(v.status());
        e.setActive(v.active());
        return map(repo.save(e));
    }

    public Optional<Driver> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public Optional<Driver> findByIdForUpdate(UUID id) {
        return repo.findByIdForUpdate(id).map(this::map);
    }

    public List<Driver> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    private Driver map(DriverEntity e) {
        return new Driver(e.getId(), e.getEmployeeNumber(), e.getFirstName(), e.getLastName(), e.getPhone(), e.getEmail(), e.getStatus(), e.isActive());
    }
}
