package com.transportlogistics.app.identity.domain.model;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

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

    public Set<UUID> roleIds() {
        return roles.stream().map(Role::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Set<String> permissions() {
        return roles.stream().filter(Role::active).flatMap(role -> role.permissions().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public String toString() {
        return "User[" +
                "id=" + id +
                ", username=" + username +
                ", email=" + email +
                ", passwordHash=***" +
                ", firstName=" + firstName +
                ", lastName=" + lastName +
                ", phone=" + phone +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", roles=" + roles +
                ']';
    }
}
