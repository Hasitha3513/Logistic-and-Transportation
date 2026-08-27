package com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.identity.domain.model.AuthTokens;

public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static AuthResponse from(AuthTokens tokens) {
        if (tokens == null) {
            return null;
        }
        return new AuthResponse(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(), tokens.expiresIn());
    }

    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, refreshToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
