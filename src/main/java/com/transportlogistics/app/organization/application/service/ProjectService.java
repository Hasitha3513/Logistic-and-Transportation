package com.transportlogistics.app.organization.application.service;

import com.transportlogistics.app.organization.application.ports.in.ProjectUseCase;
import com.transportlogistics.app.organization.application.ports.out.ProjectRepository;
import com.transportlogistics.app.organization.domain.model.Project;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

public final class ProjectService implements ProjectUseCase {
    private final ProjectRepository repo;

    public ProjectService(ProjectRepository repo) {
        this.repo = repo;
    }

    public Project create(Project value) {
        return repo.save(value);
    }

    public Project get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }

    public List<Project> list() {
        return repo.findAll();
    }

    public Project update(UUID id, Project value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Project(v.id(), v.code(), v.name(), v.departmentId(), false));
    }
}
