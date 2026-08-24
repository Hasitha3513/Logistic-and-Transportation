package com.transportlogistics.app.trip.infrastructure.adapters.out.identity;

import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import com.transportlogistics.app.trip.application.ports.out.TripActorPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class IdentityTripActorAdapter implements TripActorPort {
    private final AuthenticatedUserLookup users;

    IdentityTripActorAdapter(AuthenticatedUserLookup users) {
        this.users = users;
    }

    @Override
    public Optional<Actor> find(String username) {
        return users.findByUsername(username).map(user -> new Actor(user.id(), user.username()));
    }

    @Override
    public UUID resolveActorId(String username) {
        if (username != null && !username.isBlank() && !"system".equalsIgnoreCase(username)) {
            var found = users.findByUsername(username);
            if (found.isPresent()) {
                return found.get().id();
            }
        }
        return users.findByUsername("admin")
                .map(AuthenticatedUserLookup.AuthenticatedUser::id)
                .orElseGet(() -> UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }
}