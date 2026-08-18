package com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record RoleRequest(@NotBlank String name,
                          String description,
                          Boolean active,
                          Set<@NotBlank String> permissions) {
}
