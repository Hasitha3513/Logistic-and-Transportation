package com.transportlogistics.app.identity;

import java.util.Optional;
import java.util.UUID;

/**
 * Public identity contract for resolving an authenticated principal without exposing identity persistence.
 */
public interface AuthenticatedUserLookup {
    Optional<AuthenticatedUser> findByUsername(String username);

    record AuthenticatedUser(UUID id, String username) {
    }
}
