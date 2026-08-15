package com.transportlogistics.app.fuel.application.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface FuelActorPort {
    Optional<Actor> find(String username);

    record Actor(UUID id, String username) {
    }
}
