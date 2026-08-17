package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.DriverAssignmentAvailability;
import com.transportlogistics.app.fleet.application.ports.in.DriverAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.DriverAvailability;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.util.ArrayList;
import java.util.List;

import static com.transportlogistics.app.fleet.domain.model.DriverAvailability.Code.*;

public final class DriverAvailabilityService implements DriverAvailabilityUseCase {
    private final DriverRepository drivers;
    private final DriverLicenseRepository licenses;
    private final DriverAssignmentAvailability assignments;

    public DriverAvailabilityService(DriverRepository drivers, DriverLicenseRepository licenses,
                                     DriverAssignmentAvailability assignments) {
        this.drivers = drivers;
        this.licenses = licenses;
        this.assignments = assignments;
    }

    @Override
    public DriverAvailability evaluate(Query query) {
        if (query.from() == null || query.to() == null || !query.from().isBefore(query.to())) {
            throw new IllegalArgumentException("Availability period must have from before to");
        }
        var driver = (query.lockDriver() ? drivers.findByIdForUpdate(query.driverId())
                : drivers.findById(query.driverId()))
                .orElseThrow(() -> new NotFoundException("Driver not found: " + query.driverId()));
        var reasons = new ArrayList<DriverAvailability.Reason>();

        if (!driver.active()) add(reasons, INACTIVE, "Driver is inactive");
        if (!"AVAILABLE".equalsIgnoreCase(driver.status())) {
            add(reasons, OPERATIONALLY_UNAVAILABLE, "Driver is not operationally available");
        }

        var activeLicenses = licenses.findActiveByDriverId(driver.id());
        if (activeLicenses.isEmpty()) {
            add(reasons, LICENSE_MISSING, "Driver has no active license");
        }

        addLicenseReasonsWhenNoneQualifies(reasons, activeLicenses, query);
        if (query.checkAssignmentConflicts()
                && assignments.hasOverlap(driver.id(), query.from(), query.to(), query.excludeTripId())) {
            add(reasons, OVERLAPPING_ASSIGNMENT, "Driver has an overlapping trip assignment");
        }
        return DriverAvailability.from(reasons);
    }

    private void addLicenseReasonsWhenNoneQualifies(ArrayList<DriverAvailability.Reason> reasons,
                                                     List<DriverLicense> candidateLicenses,
                                                     Query query) {
        var requestedFrom = query.from().toLocalDate();
        var requestedTo = query.to().toLocalDate();
        if (candidateLicenses.stream().anyMatch(license -> license.isValidForPeriod(
                query.requiredLicenseClass(), requestedFrom, requestedTo))) {
            return;
        }

        var applicableLicenses = candidateLicenses.stream()
                .filter(DriverLicense::isActiveForAssignment)
                .filter(license -> license.isCompatibleWith(query.requiredLicenseClass()))
                .toList();

        if (applicableLicenses.stream().anyMatch(license -> license.issueDate().isAfter(requestedFrom))) {
            add(reasons, LICENSE_NOT_YET_VALID,
                    "No applicable active license is valid at the requested start");
        }
        if (applicableLicenses.stream().anyMatch(license -> license.expiryDate().isBefore(requestedTo))) {
            add(reasons, LICENSE_EXPIRED,
                    "No applicable active license remains valid through the requested period");
        }
        if (query.requiredLicenseClass() != null && !query.requiredLicenseClass().isBlank()) {
            add(reasons, REQUIRED_LICENSE_CLASS_MISSING,
                    "Driver lacks a valid active license of the required class for the requested period");
        }
    }

    private void add(ArrayList<DriverAvailability.Reason> reasons, DriverAvailability.Code code, String message) {
        reasons.add(new DriverAvailability.Reason(code, message));
    }
}
