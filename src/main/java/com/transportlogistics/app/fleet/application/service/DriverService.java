package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverAvailability;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DriverService implements DriverUseCase {
    private final DriverRepository repo;
    private final DriverLicenseRepository licenses;

    public DriverService(DriverRepository repo, DriverLicenseRepository licenses) {
        this.repo = repo;
        this.licenses = licenses;
    }

    public Driver create(Driver value) {
        return repo.save(value);
    }

    public Driver get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Driver not found: " + id));
    }

    public List<Driver> list() {
        return repo.findAll();
    }

    public Driver update(UUID id, Driver value) {
        return repo.save(value);
    }

    public void deactivate(UUID id) {
        var v = get(id);
        repo.save(new Driver(v.id(), v.employeeNumber(), v.firstName(), v.lastName(), v.phone(), v.email(), v.status(), false));
    }

    @Override
    public DriverAvailability availability(UUID id, String requiredLicenseClass, LocalDate onDate) {
        var driver = get(id);
        if (!driver.active()) return DriverAvailability.unavailable("INACTIVE");
        if (!"AVAILABLE".equalsIgnoreCase(driver.status())) {
            return DriverAvailability.unavailable("DRIVER_STATUS_" + driver.status().toUpperCase());
        }
        var activeLicenses = licenses.findActiveByDriverId(id);
        if (activeLicenses.stream().anyMatch(license -> license.isExpiredOn(onDate))) {
            return DriverAvailability.unavailable("LICENSE_EXPIRED");
        }
        if (requiredLicenseClass != null && !requiredLicenseClass.isBlank()
                && activeLicenses.stream().noneMatch(license -> license.isValidFor(requiredLicenseClass, onDate))) {
            return DriverAvailability.unavailable("REQUIRED_LICENSE_CLASS_MISSING_OR_EXPIRED");
        }
        return DriverAvailability.eligible();
    }

    @Override
    public void assertAvailableForAssignment(UUID id, String requiredLicenseClass, LocalDate onDate) {
        var availability = availability(id, requiredLicenseClass, onDate);
        if (!availability.available()) {
            throw new IllegalArgumentException("Driver is unavailable for assignment: " + availability.reason());
        }
    }
}
