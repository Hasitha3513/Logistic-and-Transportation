package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverViolationUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverViolationRepository;
import com.transportlogistics.app.fleet.domain.model.DriverViolation;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Transactional
public class DriverViolationService implements DriverViolationUseCase {

    private final DriverViolationRepository violations;
    private final DriverRepository drivers;

    public DriverViolationService(DriverViolationRepository violations, DriverRepository drivers) {
        this.violations = violations;
        this.drivers = drivers;
    }

    @Override
    public DriverViolation recordViolation(RecordCommand command) {
        drivers.findById(command.driverId())
                .orElseThrow(() -> new NotFoundException("Driver not found: " + command.driverId()));

        var violation = DriverViolation.record(
                command.driverId(),
                command.tripId(),
                command.violationType(),
                command.severity(),
                command.violationDate(),
                command.penaltyPoints(),
                command.fineAmount(),
                command.location(),
                command.description(),
                command.recordedBy()
        );

        return violations.save(violation);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverViolation getViolation(UUID driverId, UUID violationId) {
        return findAndVerifyOwnership(driverId, violationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverViolation> listViolations(UUID driverId) {
        drivers.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        return violations.findByDriverId(driverId);
    }

    @Override
    public DriverViolation payFine(PayCommand command) {
        var violation = findAndVerifyOwnership(command.driverId(), command.violationId());
        var paid = violation.pay(command.paidAt(), command.paymentReference(), command.updatedBy());
        return violations.save(paid);
    }

    @Override
    public DriverViolation waiveFine(WaiveCommand command) {
        var violation = findAndVerifyOwnership(command.driverId(), command.violationId());
        var waived = violation.waive(command.reason(), command.updatedBy());
        return violations.save(waived);
    }

    @Override
    public DriverViolation disputeFine(DisputeCommand command) {
        var violation = findAndVerifyOwnership(command.driverId(), command.violationId());
        var disputed = violation.dispute(command.reason(), command.updatedBy());
        return violations.save(disputed);
    }

    private DriverViolation findAndVerifyOwnership(UUID driverId, UUID violationId) {
        drivers.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        var violation = violations.findById(violationId)
                .orElseThrow(() -> new NotFoundException("Violation not found: " + violationId));

        if (!violation.driverId().equals(driverId)) {
            throw new NotFoundException("Violation " + violationId + " does not belong to driver " + driverId);
        }

        return violation;
    }
}
