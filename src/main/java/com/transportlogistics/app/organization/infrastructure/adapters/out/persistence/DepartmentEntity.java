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
@Table(name = "department")
@Getter
@Setter
@NoArgsConstructor
class DepartmentEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "code")
    private String code;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "active")
    private boolean active;

    public DepartmentEntity(UUID id, String code, String name, String description, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
    }
}
