package com.transportlogistics.app.identity.domain.model;

import java.time.OffsetDateTime;

public record IssuedRefreshToken(String value, OffsetDateTime expiresAt) {
    @Override
    public String toString() {
        return "IssuedRefreshToken[" +
                "value=***" +
                ", expiresAt=" + expiresAt +
                ']';
    }
}
