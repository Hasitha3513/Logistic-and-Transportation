package com.transportlogistics.app.organization.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "project")
@Getter
@Setter
@NoArgsConstructor
class ProjectEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "code")
    private String code;
    @Column(name = "name")
    private String name;
    @Column(name = "department_id")
    private UUID departmentId;
    @Column(name = "active")
    private boolean active;

    public ProjectEntity(UUID id, String code, String name, UUID departmentId, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.departmentId = departmentId;
        this.active = active;
    }
}
