package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverDrugTestUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverDrugTestRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.FleetOperationalNotificationPublisher;
import com.transportlogistics.app.fleet.domain.model.DriverDrugTest;
import com.transportlogistics.app.fleet.domain.model.DrugTestResult;
import com.transportlogistics.app.fleet.domain.model.DrugTestStatus;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Transactional
public class DriverDrugTestService implements DriverDrugTestUseCase {
    private static final Logger log = LoggerFactory.getLogger(DriverDrugTestService.class);

    private final DriverRepository drivers;
    private final DriverDrugTestRepository drugTests;
    private final FleetOperationalNotificationPublisher notifications;

    public DriverDrugTestService(DriverRepository drivers, DriverDrugTestRepository drugTests,
                                 FleetOperationalNotificationPublisher notifications) {
        this.drivers = Objects.requireNonNull(drivers, "DriverRepository cannot be null");
        this.drugTests = Objects.requireNonNull(drugTests, "DriverDrugTestRepository cannot be null");
        this.notifications = Objects.requireNonNull(notifications, "Notification publisher cannot be null");
    }

    public DriverDrugTestService(DriverRepository drivers, DriverDrugTestRepository drugTests) {
        this(drivers, drugTests, event -> {});
    }

    @Override
    public DriverDrugTest schedule(UUID driverId, ScheduleCommand command, String actor) {
        var driver = drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        var now = OffsetDateTime.now();
        var test = new DriverDrugTest(
                UUID.randomUUID(),
                driver.id(),
                command.testType(),
                command.scheduledDate(),
                null,
                null,
                DrugTestResult.PENDING,
                DrugTestStatus.SCHEDULED,
                command.laboratoryOrProvider(),
                command.referenceNumber(),
                command.remarks(),
                false,
                null,
                true,
                now,
                now,
                actor,
                actor
        );
        return drugTests.save(test);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverDrugTest> list(UUID driverId) {
        drivers.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        return drugTests.findByDriverId(driverId);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverDrugTest get(UUID driverId, UUID testId) {
        return drugTests.findById(testId)
                .filter(t -> t.driverId().equals(driverId))
                .orElseThrow(() -> new NotFoundException("Drug test not found: " + testId));
    }

    @Override
    public DriverDrugTest recordSample(UUID driverId, UUID testId, RecordSampleCommand command, String actor) {
        drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        var existing = get(driverId, testId);
        if (existing.status() == DrugTestStatus.COMPLETED || existing.status() == DrugTestStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot record sample on " + existing.status() + " test");
        }

        var sampleTime = command.sampleCollectedAt() != null ? command.sampleCollectedAt() : OffsetDateTime.now();
        var updated = new DriverDrugTest(
                existing.id(),
                existing.driverId(),
                existing.testType(),
                existing.scheduledDate(),
                sampleTime,
                existing.resultDate(),
                existing.result(),
                DrugTestStatus.SAMPLE_COLLECTED,
                existing.laboratoryOrProvider(),
                existing.referenceNumber(),
                existing.remarks(),
                existing.returnToDutyRequired(),
                existing.returnToDutyClearedAt(),
                existing.active(),
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return drugTests.save(updated);
    }

    @Override
    public DriverDrugTest recordResult(UUID driverId, UUID testId, RecordResultCommand command, String actor) {
        var driver = drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        var existing = get(driverId, testId);
        if (existing.status() == DrugTestStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot record result on CANCELLED test");
        }
        Objects.requireNonNull(command.result(), "Drug test result cannot be null");

        boolean rtdRequired = command.result() == DrugTestResult.POSITIVE || (command.returnToDutyRequired() != null && command.returnToDutyRequired());
        var updated = new DriverDrugTest(
                existing.id(),
                existing.driverId(),
                existing.testType(),
                existing.scheduledDate(),
                existing.sampleCollectedAt(),
                command.resultDate() != null ? command.resultDate() : java.time.LocalDate.now(),
                command.result(),
                DrugTestStatus.COMPLETED,
                existing.laboratoryOrProvider(),
                existing.referenceNumber(),
                command.remarks() != null ? command.remarks() : existing.remarks(),
                rtdRequired,
                null,
                existing.active(),
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        var saved = drugTests.save(updated);
        if (saved.isBlocking()) {
            publishSafely(FleetOperationalNotificationEvents.drugTestFailed(saved, driver, saved.updatedAt()));
        }
        return saved;
    }

    @Override
    public DriverDrugTest clearReturnToDuty(UUID driverId, UUID testId, ReturnToDutyClearanceCommand command, String actor) {
        drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        var existing = get(driverId, testId);
        if (!existing.returnToDutyRequired()) {
            throw new BusinessRuleException("INVALID_STATE", "Return-to-duty clearance is not required for this test");
        }

        var clearedAt = command.clearedAt() != null ? command.clearedAt() : OffsetDateTime.now();
        var remarks = existing.remarks();
        if (command.remarks() != null && !command.remarks().isBlank()) {
            remarks = remarks != null ? remarks + " | RTD Cleared: " + command.remarks() : "RTD Cleared: " + command.remarks();
        }

        var updated = new DriverDrugTest(
                existing.id(),
                existing.driverId(),
                existing.testType(),
                existing.scheduledDate(),
                existing.sampleCollectedAt(),
                existing.resultDate(),
                existing.result(),
                existing.status(),
                existing.laboratoryOrProvider(),
                existing.referenceNumber(),
                remarks,
                existing.returnToDutyRequired(),
                clearedAt,
                existing.active(),
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return drugTests.save(updated);
    }

    @Override
    public DriverDrugTest cancel(UUID driverId, UUID testId, String reason, String actor) {
        drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        var existing = get(driverId, testId);
        if (existing.status() == DrugTestStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot cancel a completed drug test");
        }

        var updated = new DriverDrugTest(
                existing.id(),
                existing.driverId(),
                existing.testType(),
                existing.scheduledDate(),
                existing.sampleCollectedAt(),
                existing.resultDate(),
                existing.result(),
                DrugTestStatus.CANCELLED,
                existing.laboratoryOrProvider(),
                existing.referenceNumber(),
                reason != null ? existing.remarks() + " (Cancelled: " + reason + ")" : existing.remarks(),
                false,
                null,
                false,
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return drugTests.save(updated);
    }

    private void publishSafely(com.transportlogistics.app.notification.OperationalNotificationEvent event) {
        try {
            notifications.publish(event);
        } catch (RuntimeException exception) {
            log.error("Driver drug-test notification publication failed for event {}", event.eventId(), exception);
        }
    }
}
