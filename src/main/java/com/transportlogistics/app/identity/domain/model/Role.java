package com.transportlogistics.app.identity.domain.model;

import java.util.Set;
import java.util.UUID;

public record Role(UUID id, String name, String description, boolean active, Set<String> permissions) {
    public Role {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean grants(String permission) {
        return active && permissions.contains(permission);
    }
}
