package com.transportlogistics.app.trip.infrastructure.adapters.out.identity;

import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import com.transportlogistics.app.trip.application.ports.out.TripActorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class IdentityTripActorAdapter implements TripActorPort {
    private final AuthenticatedUserLookup users;

    @Override
    public Optional<Actor> find(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return users.findByUsername(username.trim())
                .map(user -> new Actor(user.id(), user.username()));
    }
}
