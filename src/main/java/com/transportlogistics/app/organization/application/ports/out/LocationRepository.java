package com.transportlogistics.app.organization.application.ports.out;

import com.transportlogistics.app.organization.domain.model.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository {
    Location save(Location value);

    Optional<Location> findById(UUID id);

    List<Location> findAll();
}
