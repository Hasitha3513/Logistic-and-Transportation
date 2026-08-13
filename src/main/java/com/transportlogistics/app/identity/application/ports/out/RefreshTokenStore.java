package com.transportlogistics.app.identity.application.ports.out;

import com.transportlogistics.app.identity.domain.model.IssuedRefreshToken;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {
    IssuedRefreshToken issue(UUID userId, OffsetDateTime expiresAt);

    Optional<Rotation> rotate(String token, OffsetDateTime now, OffsetDateTime newExpiresAt);

    boolean revoke(String token, OffsetDateTime revokedAt);

    record Rotation(UUID userId, IssuedRefreshToken token) {
    }
}
