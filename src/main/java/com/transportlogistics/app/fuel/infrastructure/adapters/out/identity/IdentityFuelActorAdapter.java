package com.transportlogistics.app.fuel.infrastructure.adapters.out.identity;

import com.transportlogistics.app.fuel.application.ports.out.FuelActorPort;
import com.transportlogistics.app.identity.AuthenticatedUserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class IdentityFuelActorAdapter implements FuelActorPort {
    private final AuthenticatedUserLookup users;

    @Override
    public Optional<Actor> find(String username) {
        return users.findByUsername(username).map(user -> new Actor(user.id(), user.username()));
    }
}
