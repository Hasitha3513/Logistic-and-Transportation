package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.Driver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

public interface DriverRepository {
    Driver save(Driver value);

    Optional<Driver> findById(UUID id);

    Optional<Driver> findByIdForUpdate(UUID id);

    List<Driver> findAll();

    default List<Driver> findAllByIds(Set<UUID> ids) {
        return findAll().stream().filter(driver -> ids.contains(driver.id())).toList();
    }
}
