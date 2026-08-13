package com.transportlogistics.app.identity.infrastructure.adapters.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "app_role")
@Getter
@Setter
@NoArgsConstructor
class RoleEntity {
    @Id
    private UUID id;
    private String name;
    private String description;
    private boolean active;
}