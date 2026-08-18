package com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=***]";
    }
}
