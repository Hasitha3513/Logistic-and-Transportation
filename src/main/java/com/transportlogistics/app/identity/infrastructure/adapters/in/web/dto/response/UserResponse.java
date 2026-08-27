package com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.identity.domain.model.User;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id,
                           String username,
                           String email,
                           String firstName,
                           String lastName,
                           String phone,
                           boolean active,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt,
                           Set<String> roles,
                           Set<String> permissions,
                           Set<UUID> roleIds) {

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(user.id(), user.username(), user.email(), user.firstName(), user.lastName(),
                user.phone(), user.active(), user.createdAt(), user.updatedAt(), user.roleNames(), user.permissions(), user.roleIds());
    }
}
