package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.DriverMedicalRecord;
import com.transportlogistics.app.fleet.domain.model.DriverMedicalStatus;
import com.transportlogistics.app.fleet.domain.model.VisionTestStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DriverMedicalRecordUseCase {

    DriverMedicalRecord create(UUID driverId, CreateCommand command, String actor);

    List<DriverMedicalRecord> list(UUID driverId);

    DriverMedicalRecord get(UUID driverId, UUID recordId);

    DriverMedicalRecord update(UUID driverId, UUID recordId, UpdateCommand command, String actor);

    record CreateCommand(
            LocalDate assessmentDate,
            LocalDate validFrom,
            LocalDate validUntil,
            DriverMedicalStatus fitnessStatus,
            VisionTestStatus visionTestStatus,
            String restrictions,
            String examinerOrProvider,
            String certificateReference,
            String remarks
    ) {}

    record UpdateCommand(
            LocalDate assessmentDate,
            LocalDate validFrom,
            LocalDate validUntil,
            DriverMedicalStatus fitnessStatus,
            VisionTestStatus visionTestStatus,
            String restrictions,
            String examinerOrProvider,
            String certificateReference,
            String remarks,
            Boolean active
    ) {}
}
