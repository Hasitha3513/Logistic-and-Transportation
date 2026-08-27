package com.transportlogistics.app.identity.domain.model;

public record AuthTokens(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    @Override
    public String toString() {
        return "AuthTokens[" +
                "accessToken=***" +
                ", refreshToken=***" +
                ", tokenType=" + tokenType +
                ", expiresIn=" + expiresIn +
                ']';
    }
}
