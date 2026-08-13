package com.transportlogistics.app.organization.application.service;

import com.transportlogistics.app.organization.application.ports.in.DepartmentUseCase;
import com.transportlogistics.app.organization.application.ports.out.DepartmentRepository;
import com.transportlogistics.app.organization.domain.model.Department;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class DepartmentService implements DepartmentUseCase {
    private final DepartmentRepository repo;

    public DepartmentService(DepartmentRepository repo) {
        this.repo = repo;
    }

    public Department create(Department value) {
        return repo.save(value);
    }

    public Department get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Department not found: " + id));
    }

    public List<Department> list() {
        return repo.findAll();
    }

    public Department update(UUID id, Department value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Department(v.id(), v.code(), v.name(), v.description(), false));
    }
}
