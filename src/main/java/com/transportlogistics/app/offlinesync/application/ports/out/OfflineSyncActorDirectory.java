package com.transportlogistics.app.offlinesync.application.ports.out;

import java.util.Optional;
import java.util.UUID;

public interface OfflineSyncActorDirectory {
    Optional<Actor> findByUsername(String username);

    record Actor(UUID id, String username) {
    }
}
