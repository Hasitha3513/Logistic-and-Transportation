package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.organization.application.ports.out.ProjectRepository;
import com.transportlogistics.app.organization.domain.model.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProjectPersistenceAdapter implements ProjectRepository {
    private final ProjectJpaRepository repo;

    public Project save(Project v) {
        var e = new ProjectEntity();
        e.setId(v.id());
        e.setCode(v.code());
        e.setName(v.name());
        e.setDepartmentId(v.departmentId());
        e.setActive(v.active());
        return map(repo.save(e));
    }

    public Optional<Project> findById(UUID id) {
        return repo.findById(id).map(this::map);
    }

    public List<Project> findAll() {
        return repo.findAll().stream().map(this::map).toList();
    }

    private Project map(ProjectEntity e) {
        return new Project(e.getId(), e.getCode(), e.getName(), e.getDepartmentId(), e.isActive());
    }
}
