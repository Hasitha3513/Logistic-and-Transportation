package com.transportlogistics.app.offlinesync.infrastructure.adapters.out.identity;

import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import com.transportlogistics.app.offlinesync.application.ports.out.OfflineSyncActorDirectory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class IdentityOfflineSyncActorAdapter implements OfflineSyncActorDirectory {
    private final AuthenticatedUserLookup users;

    IdentityOfflineSyncActorAdapter(AuthenticatedUserLookup users) {
        this.users = users;
    }

    @Override
    public Optional<Actor> findByUsername(String username) {
        return users.findByUsername(username).map(user -> new Actor(user.id(), user.username()));
    }
}
