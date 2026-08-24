package com.transportlogistics.app.identity.domain.model;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record TokenClaims(UUID userId, String username, Set<String> roles, Set<String> permissions,
                          OffsetDateTime expiresAt) {
    public TokenClaims {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}
