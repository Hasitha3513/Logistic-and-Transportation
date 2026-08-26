package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.organization.application.ports.out.DepartmentRepository;
import com.transportlogistics.app.organization.domain.model.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class DepartmentPersistenceAdapter implements DepartmentRepository {
    private final DepartmentJpaRepository repo;

    public Department save(Department v) {
        var e = new DepartmentEntity();
        e.setId(v.id());
        e.setCode(v.code());
        e.setName(v.name());
        e.setDescription(v.description());
        e.setActive(v.active());
        return map(repo.save(e));
    }

    public Optional<Department> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public List<Department> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    private Department map(DepartmentEntity e) {
        return new Department(e.getId(), e.getCode(), e.getName(), e.getDescription(), e.isActive());
    }
}
