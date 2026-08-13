package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DriverUseCase {
    Driver create(Driver value);

    Driver get(UUID id);

    List<Driver> list();

    Driver update(UUID id, Driver value);

    void deactivate(UUID id);

    DriverAvailability availability(UUID id, String requiredLicenseClass, LocalDate onDate);

    void assertAvailableForAssignment(UUID id, String requiredLicenseClass, LocalDate onDate);
}
