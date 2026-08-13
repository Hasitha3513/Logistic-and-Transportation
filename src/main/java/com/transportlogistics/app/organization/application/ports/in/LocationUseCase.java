package com.transportlogistics.app.organization.application.ports.in;

import com.transportlogistics.app.organization.domain.model.Location;

import java.util.List;
import java.util.UUID;

public interface LocationUseCase {
    Location create(Location value);

    Location get(UUID id);

    List<Location> list();

    Location update(UUID id, Location value);

    void deactivate(UUID id);
}
