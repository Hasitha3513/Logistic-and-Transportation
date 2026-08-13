package com.transportlogistics.app.identity.domain.model;

import java.time.OffsetDateTime;

public record IssuedRefreshToken(String value, OffsetDateTime expiresAt) {
}
