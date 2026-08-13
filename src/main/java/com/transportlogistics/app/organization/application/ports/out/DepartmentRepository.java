package com.transportlogistics.app.organization.application.ports.out;

import com.transportlogistics.app.organization.domain.model.Department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {
    Department save(Department value);

    Optional<Department> findById(UUID id);

    List<Department> findAll();
}
