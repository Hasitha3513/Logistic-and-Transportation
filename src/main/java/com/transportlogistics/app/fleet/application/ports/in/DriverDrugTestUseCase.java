package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.DriverDrugTest;
import com.transportlogistics.app.fleet.domain.model.DrugTestResult;
import com.transportlogistics.app.fleet.domain.model.DrugTestType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DriverDrugTestUseCase {

    DriverDrugTest schedule(UUID driverId, ScheduleCommand command, String actor);

    List<DriverDrugTest> list(UUID driverId);

    DriverDrugTest get(UUID driverId, UUID testId);

    DriverDrugTest recordSample(UUID driverId, UUID testId, RecordSampleCommand command, String actor);

    DriverDrugTest recordResult(UUID driverId, UUID testId, RecordResultCommand command, String actor);

    DriverDrugTest clearReturnToDuty(UUID driverId, UUID testId, ReturnToDutyClearanceCommand command, String actor);

    DriverDrugTest cancel(UUID driverId, UUID testId, String reason, String actor);

    record ScheduleCommand(
            DrugTestType testType,
            LocalDate scheduledDate,
            String laboratoryOrProvider,
            String referenceNumber,
            String remarks
    ) {}

    record RecordSampleCommand(
            OffsetDateTime sampleCollectedAt
    ) {}

    record RecordResultCommand(
            DrugTestResult result,
            LocalDate resultDate,
            String remarks,
            Boolean returnToDutyRequired
    ) {}

    record ReturnToDutyClearanceCommand(
            OffsetDateTime clearedAt,
            String remarks
    ) {}
}
