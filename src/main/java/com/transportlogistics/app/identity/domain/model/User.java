package com.transportlogistics.app.identity.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Set;

public record User(UUID id, String username, String email, String passwordHash, String firstName, String lastName,
                   String phone, boolean active, OffsetDateTime createdAt, OffsetDateTime updatedAt, Set<Role> roles) {
    public User {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasRole(String roleName) {
        return active && roles.stream().anyMatch(role -> role.active() && role.name().equalsIgnoreCase(roleName));
    }

    public boolean hasPermission(String permission) {
        return active && roles.stream().anyMatch(role -> role.grants(permission));
    }

    public Set<String> roleNames() {
        return roles.stream().filter(Role::active).map(Role::name).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Set<String> permissions() {
        return roles.stream().filter(Role::active).flatMap(role -> role.permissions().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
