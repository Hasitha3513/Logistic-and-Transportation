package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "vehicle_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class VehicleCategoryEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
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
}
