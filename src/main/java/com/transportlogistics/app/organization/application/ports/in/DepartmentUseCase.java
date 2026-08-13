package com.transportlogistics.app.organization.application.ports.in;

import com.transportlogistics.app.organization.domain.model.Department;

import java.util.List;
import java.util.UUID;

public interface DepartmentUseCase {
    Department create(Department value);

    Department get(UUID id);

    List<Department> list();

    Department update(UUID id, Department value);

    void deactivate(UUID id);
}
