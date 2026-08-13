package com.transportlogistics.app.organization.application.ports.in;

import com.transportlogistics.app.organization.domain.model.Project;

import java.util.List;
import java.util.UUID;

public interface ProjectUseCase {
    Project create(Project value);

    Project get(UUID id);

    List<Project> list();

    Project update(UUID id, Project value);

    void deactivate(UUID id);
}
