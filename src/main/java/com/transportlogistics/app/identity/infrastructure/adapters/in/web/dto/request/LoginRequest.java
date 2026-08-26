package com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
