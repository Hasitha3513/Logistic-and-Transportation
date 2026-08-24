package com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.identity.domain.model.Role;

import java.util.Set;
import java.util.UUID;

public record RoleResponse(UUID id,
                           String name,
                           String description,
                           boolean active,
                           Set<String> permissions) {

    public static RoleResponse from(Role role) {
        if (role == null) {
            return null;
        }
        return new RoleResponse(role.id(), role.name(), role.description(), role.active(), role.permissions());
    }
}
