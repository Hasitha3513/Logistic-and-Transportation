package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.DriverDrugTestUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverDrugTestRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.FleetOperationalNotificationPublisher;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverDrugTest;
import com.transportlogistics.app.fleet.domain.model.DrugTestResult;
import com.transportlogistics.app.fleet.domain.model.DrugTestStatus;
import com.transportlogistics.app.fleet.domain.model.DrugTestType;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverDrugTestServiceTest {

    @Mock
    private DriverRepository drivers;

    @Mock
    private DriverDrugTestRepository drugTests;

    @Mock
    private FleetOperationalNotificationPublisher notifications;

    private DriverDrugTestService service;
    private UUID driverId;
    private Driver driver;

    @BeforeEach
    void setUp() {
        service = new DriverDrugTestService(drivers, drugTests, notifications);
        driverId = UUID.randomUUID();
        driver = new Driver(driverId, "EMP-001", "John", "Doe", "+1234567890", "john@example.com", "AVAILABLE", true);
    }

    @Test
    @DisplayName("Should schedule drug test for driver")
    void shouldScheduleDrugTest() {
        when(drivers.findByIdForUpdate(driverId)).thenReturn(Optional.of(driver));
        when(drugTests.save(any(DriverDrugTest.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new DriverDrugTestUseCase.ScheduleCommand(
                DrugTestType.RANDOM,
                LocalDate.of(2026, 6, 1),
                "LabCorp",
                "REF-001",
                "Quarterly test"
        );

        var result = service.schedule(driverId, command, "admin");

        assertNotNull(result);
        assertEquals(DrugTestStatus.SCHEDULED, result.status());
        assertEquals(DrugTestResult.PENDING, result.result());
        verify(drugTests).save(any(DriverDrugTest.class));
    }

    @Test
    @DisplayName("Should record positive result and require return-to-duty clearance")
    void shouldRecordPositiveResult() {
        var testId = UUID.randomUUID();
        var scheduledTest = new DriverDrugTest(
                testId,
                driverId,
                DrugTestType.RANDOM,
                LocalDate.of(2026, 6, 1),
                OffsetDateTime.now(),
                null,
                DrugTestResult.PENDING,
                DrugTestStatus.SAMPLE_COLLECTED,
                "LabCorp",
                "REF-001",
                null,
                false,
                null,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "admin",
                "admin"
        );

        when(drivers.findByIdForUpdate(driverId)).thenReturn(Optional.of(driver));
        when(drugTests.findById(testId)).thenReturn(Optional.of(scheduledTest));
        when(drugTests.save(any(DriverDrugTest.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new DriverDrugTestUseCase.RecordResultCommand(
                DrugTestResult.POSITIVE,
                LocalDate.of(2026, 6, 2),
                "Positive for THC",
                true
        );

        var result = service.recordResult(driverId, testId, command, "admin");

        assertEquals(DrugTestResult.POSITIVE, result.result());
        assertEquals(DrugTestStatus.COMPLETED, result.status());
        assertTrue(result.returnToDutyRequired());
        assertNull(result.returnToDutyClearedAt());
        assertTrue(result.isBlocking());
        var captor = org.mockito.ArgumentCaptor.forClass(
                com.transportlogistics.app.notification.OperationalNotificationEvent.class);
        verify(notifications).publish(captor.capture());
        assertEquals("DRIVER_DRUG_TEST_FAILED", captor.getValue().eventType());
        assertEquals(com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.CRITICAL,
                captor.getValue().severity());
        assertEquals(testId.toString(), captor.getValue().metadata().get("drugTestId"));
        assertEquals("John Doe", captor.getValue().metadata().get("driverName"));
        assertEquals("RANDOM", captor.getValue().metadata().get("testType"));
    }

    @Test
    void negativeResultDoesNotPublishAndPublisherFailureDoesNotUndoPositiveResult() {
        var testId = UUID.randomUUID();
        var existing = new DriverDrugTest(testId, driverId, DrugTestType.POST_INCIDENT,
                LocalDate.of(2026, 6, 1), OffsetDateTime.now(), null, DrugTestResult.PENDING,
                DrugTestStatus.SAMPLE_COLLECTED, "Lab", "REF", null, false, null, true,
                OffsetDateTime.now(), OffsetDateTime.now(), "admin", "admin");
        when(drivers.findByIdForUpdate(driverId)).thenReturn(Optional.of(driver));
        when(drugTests.findById(testId)).thenReturn(Optional.of(existing));
        when(drugTests.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordResult(driverId, testId, new DriverDrugTestUseCase.RecordResultCommand(
                DrugTestResult.NEGATIVE, LocalDate.of(2026, 6, 2), null, false), "admin");
        verifyNoInteractions(notifications);

        doThrow(new IllegalStateException("listener failed")).when(notifications).publish(any());
        var positive = service.recordResult(driverId, testId, new DriverDrugTestUseCase.RecordResultCommand(
                DrugTestResult.POSITIVE, LocalDate.of(2026, 6, 2), null, true), "admin");
        assertTrue(positive.isBlocking());
    }

    @Test
    @DisplayName("Should clear return-to-duty for positive test")
    void shouldClearReturnToDuty() {
        var testId = UUID.randomUUID();
        var positiveTest = new DriverDrugTest(
                testId,
                driverId,
                DrugTestType.RANDOM,
                LocalDate.of(2026, 6, 1),
                OffsetDateTime.now(),
                LocalDate.of(2026, 6, 2),
                DrugTestResult.POSITIVE,
                DrugTestStatus.COMPLETED,
                "LabCorp",
                "REF-001",
                "Positive for THC",
                true,
                null,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "admin",
                "admin"
        );

        when(drivers.findByIdForUpdate(driverId)).thenReturn(Optional.of(driver));
        when(drugTests.findById(testId)).thenReturn(Optional.of(positiveTest));
        when(drugTests.save(any(DriverDrugTest.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new DriverDrugTestUseCase.ReturnToDutyClearanceCommand(
                OffsetDateTime.now(),
                "Completed substance abuse program and negative follow-up test"
        );

        var result = service.clearReturnToDuty(driverId, testId, command, "compliance-officer");

        assertNotNull(result.returnToDutyClearedAt());
        assertFalse(result.isBlocking());
    }
}
