package com.transportlogistics.app.trip.application.ports.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for resolving an authenticated actor identity in the Trip module.
 */
public interface TripActorPort {
    Optional<Actor> find(String username);

    record Actor(UUID id, String username) {
    }
}
