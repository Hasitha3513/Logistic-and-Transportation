package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.DriverViolation;
import com.transportlogistics.app.fleet.domain.model.DriverViolationType;
import com.transportlogistics.app.fleet.domain.model.ViolationSeverity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DriverViolationUseCase {

    record RecordCommand(
            UUID driverId,
            UUID tripId,
            DriverViolationType violationType,
            ViolationSeverity severity,
            OffsetDateTime violationDate,
            int penaltyPoints,
            BigDecimal fineAmount,
            String location,
            String description,
            String recordedBy
    ) {}

    record PayCommand(
            UUID driverId,
            UUID violationId,
            OffsetDateTime paidAt,
            String paymentReference,
            String updatedBy
    ) {}

    record WaiveCommand(
            UUID driverId,
            UUID violationId,
            String reason,
            String updatedBy
    ) {}

    record DisputeCommand(
            UUID driverId,
            UUID violationId,
            String reason,
            String updatedBy
    ) {}

    DriverViolation recordViolation(RecordCommand command);

    DriverViolation getViolation(UUID driverId, UUID violationId);

    List<DriverViolation> listViolations(UUID driverId);

    DriverViolation payFine(PayCommand command);

    DriverViolation waiveFine(WaiveCommand command);

    DriverViolation disputeFine(DisputeCommand command);
}
