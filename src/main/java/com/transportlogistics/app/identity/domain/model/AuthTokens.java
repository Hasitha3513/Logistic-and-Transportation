package com.transportlogistics.app.identity.domain.model;

public record AuthTokens(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
