package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverLicenseUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DriverLicenseService implements DriverLicenseUseCase {
    private final DriverRepository drivers;
    private final DriverLicenseRepository licenses;

    public DriverLicenseService(DriverRepository drivers, DriverLicenseRepository licenses) {
        this.drivers = drivers;
        this.licenses = licenses;
    }

    @Override
    public List<DriverLicense> list(UUID driverId) {
        requireDriver(driverId);
        return licenses.findVisibleByDriverId(driverId);
    }

    @Override
    public DriverLicense create(UUID driverId, CreateCommand command, String actor) {
        requireDriver(driverId);
        var state = state(command.status(), command.active(), DriverLicenseStatus.ACTIVE, true);
        rejectDeletedStatus(state.status());
        var now = OffsetDateTime.now();
        var license = new DriverLicense(UUID.randomUUID(), driverId, command.licenseNumber(), command.licenseClass(),
                command.issueDate(), command.expiryDate(), state.status(), state.active(), now, now,
                actor(actor), actor(actor));
        rejectDuplicateNumber(license.licenseNumber(), null);
        return licenses.save(license);
    }

    @Override
    public DriverLicense update(UUID driverId, UUID licenseId, UpdateCommand command, String actor) {
        requireDriver(driverId);
        var current = requireLicense(driverId, licenseId);
        if (current.status() == DriverLicenseStatus.DELETED) {
            throw new NotFoundException("Driver license not found: " + licenseId);
        }
        var state = state(command.status(), command.active(), current.status(), current.active());
        rejectDeletedStatus(state.status());
        var updated = new DriverLicense(current.id(), current.driverId(),
                value(command.licenseNumber(), current.licenseNumber()),
                value(command.licenseClass(), current.licenseClass()),
                command.issueDate() == null ? current.issueDate() : command.issueDate(),
                command.expiryDate() == null ? current.expiryDate() : command.expiryDate(),
                state.status(), state.active(), current.createdAt(), OffsetDateTime.now(), current.createdBy(),
                actor(actor));
        rejectDuplicateNumber(updated.licenseNumber(), licenseId);
        return licenses.save(updated);
    }

    @Override
    public void delete(UUID driverId, UUID licenseId, String actor) {
        requireDriver(driverId);
        var current = requireLicense(driverId, licenseId);
        if (current.status() == DriverLicenseStatus.DELETED) return;
        licenses.save(new DriverLicense(current.id(), current.driverId(), current.licenseNumber(),
                current.licenseClass(), current.issueDate(), current.expiryDate(), DriverLicenseStatus.DELETED,
                false, current.createdAt(), OffsetDateTime.now(), current.createdBy(), actor(actor)));
    }

    private void requireDriver(UUID driverId) {
        if (drivers.findById(driverId).isEmpty()) {
            throw new NotFoundException("Driver not found: " + driverId);
        }
    }

    private DriverLicense requireLicense(UUID driverId, UUID licenseId) {
        var license = licenses.findById(licenseId)
                .orElseThrow(() -> new NotFoundException("Driver license not found: " + licenseId));
        if (!driverId.equals(license.driverId())) {
            throw new NotFoundException("Driver license not found: " + licenseId);
        }
        return license;
    }

    private void rejectDuplicateNumber(String number, UUID excludedId) {
        if (licenses.licenseNumberExists(number, excludedId)) {
            throw new IllegalArgumentException("License number already exists");
        }
    }

    private void rejectDeletedStatus(DriverLicenseStatus status) {
        if (status == DriverLicenseStatus.DELETED) {
            throw new IllegalArgumentException("Use the delete operation to delete a driver license");
        }
    }

    private State state(DriverLicenseStatus requestedStatus, Boolean requestedActive,
                        DriverLicenseStatus currentStatus, boolean currentActive) {
        if (requestedStatus == null && requestedActive == null) return new State(currentStatus, currentActive);
        var status = requestedStatus != null ? requestedStatus
                : requestedActive ? DriverLicenseStatus.ACTIVE : DriverLicenseStatus.INACTIVE;
        var active = requestedActive != null ? requestedActive : status == DriverLicenseStatus.ACTIVE;
        if (active != (status == DriverLicenseStatus.ACTIVE)) {
            throw new IllegalArgumentException("Active flag must match license status");
        }
        return new State(status, active);
    }

    private String value(String requested, String current) {
        return requested == null ? current : requested;
    }

    private String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor;
    }

    private record State(DriverLicenseStatus status, boolean active) {
    }
}
