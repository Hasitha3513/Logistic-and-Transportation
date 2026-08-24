package com.transportlogistics.app.trip.application.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface TripActorPort {
    Optional<Actor> find(String username);
    UUID resolveActorId(String username);

    record Actor(UUID id, String username) {
    }
}