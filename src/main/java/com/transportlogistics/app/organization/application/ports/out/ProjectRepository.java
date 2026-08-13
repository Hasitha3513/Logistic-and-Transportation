package com.transportlogistics.app.organization.application.ports.out;

import com.transportlogistics.app.organization.domain.model.Project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {
    Project save(Project value);

    Optional<Project> findById(UUID id);

    List<Project> findAll();
}
