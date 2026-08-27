package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.DriverAssignmentAvailability;
import com.transportlogistics.app.fleet.application.ports.in.DriverExceptionUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverExceptionRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.FleetOperationalNotificationPublisher;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverException;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionType;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class DriverExceptionServiceTest {

    private DriverExceptionRepository driverExceptions;
    private DriverRepository drivers;
    private DriverAssignmentAvailability assignments;
    private DriverExceptionService service;
    private FleetOperationalNotificationPublisher notifications;

    private final UUID driverId = UUID.randomUUID();
    private final Driver driver = new Driver(
            driverId, "EMP-1001", "John", "Doe", "+1234567890", "john@example.com", "AVAILABLE", true
    );

    @BeforeEach
    void setUp() {
        driverExceptions = mock(DriverExceptionRepository.class);
        drivers = mock(DriverRepository.class);
        assignments = mock(DriverAssignmentAvailability.class);
        notifications = mock(FleetOperationalNotificationPublisher.class);
        service = new DriverExceptionService(driverExceptions, drivers, assignments, notifications);

        when(drivers.findById(driverId)).thenReturn(Optional.of(driver));
        when(drivers.findByIdForUpdate(driverId)).thenReturn(Optional.of(driver));
    }

    @Test
    void createsValidDriverException() {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var command = new DriverExceptionUseCase.CreateCommand(
                DriverExceptionType.LEAVE, start, end, "Annual leave", "Personal"
        );

        when(driverExceptions.save(any(DriverException.class))).thenAnswer(inv -> inv.getArgument(0));

        var created = service.create(driverId, command, "fleet.manager");

        assertThat(created).isNotNull();
        assertThat(created.driverId()).isEqualTo(driverId);
        assertThat(created.exceptionType()).isEqualTo(DriverExceptionType.LEAVE);
        assertThat(created.status()).isEqualTo(DriverExceptionStatus.SCHEDULED);
        assertThat(created.createdBy()).isEqualTo("fleet.manager");
        verify(driverExceptions, times(1)).save(any(DriverException.class));
        var captor = org.mockito.ArgumentCaptor.forClass(
                com.transportlogistics.app.notification.OperationalNotificationEvent.class);
        verify(notifications).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("DRIVER_EXCEPTION_RECORDED");
        assertThat(captor.getValue().severity())
                .isEqualTo(com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.WARNING);
        assertThat(captor.getValue().metadata()).containsEntry("driverName", "John Doe")
                .containsEntry("exceptionType", "LEAVE")
                .containsEntry("reason", "Annual leave");
    }

    @Test
    void rejectsCreationWhenDriverNotFound() {
        var missingDriverId = UUID.randomUUID();
        when(drivers.findById(missingDriverId)).thenReturn(Optional.empty());
        when(drivers.findByIdForUpdate(missingDriverId)).thenReturn(Optional.empty());

        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var command = new DriverExceptionUseCase.CreateCommand(DriverExceptionType.LEAVE, start, end, null, null);

        assertThatThrownBy(() -> service.create(missingDriverId, command, "actor"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Driver not found");
    }

    @Test
    void rejectsCreationWhenDatesInvalid() {
        var start = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var command = new DriverExceptionUseCase.CreateCommand(DriverExceptionType.LEAVE, start, end, null, null);

        assertThatThrownBy(() -> service.create(driverId, command, "actor"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCreationWhenOverlappingActiveExceptionExists() {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var command = new DriverExceptionUseCase.CreateCommand(DriverExceptionType.LEAVE, start, end, null, null);

        when(driverExceptions.hasOverlappingException(eq(driverId), eq(start), eq(end), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(driverId, command, "actor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has a scheduled exception or leave");
    }

    @Test
    void rejectsCreationWhenOverlappingActiveTripAssignmentExists_RuleB() {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var command = new DriverExceptionUseCase.CreateCommand(DriverExceptionType.LEAVE, start, end, null, null);

        when(assignments.hasOverlap(eq(driverId), eq(start), eq(end), isNull()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(driverId, command, "actor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active trip assignment");
    }

    @Test
    void retrievesExceptionSuccessfully() {
        var exceptionId = UUID.randomUUID();
        var exception = new DriverException(
                exceptionId, driverId, DriverExceptionType.MEDICAL_EMERGENCY,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
                DriverExceptionStatus.SCHEDULED, null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.findById(exceptionId)).thenReturn(Optional.of(exception));

        var result = service.get(driverId, exceptionId);
        assertThat(result).isEqualTo(exception);
    }

    @Test
    void rejectsGetWhenDriverMismatch() {
        var exceptionId = UUID.randomUUID();
        var exception = new DriverException(
                exceptionId, UUID.randomUUID(), DriverExceptionType.LEAVE,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
                DriverExceptionStatus.SCHEDULED, null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.findById(exceptionId)).thenReturn(Optional.of(exception));

        assertThatThrownBy(() -> service.get(driverId, exceptionId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("does not belong to driver");
    }

    @Test
    void updatesAndReschedulesSuccessfully() {
        var exceptionId = UUID.randomUUID();
        var existing = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                DriverExceptionStatus.SCHEDULED, "Old reason", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.findById(exceptionId)).thenReturn(Optional.of(existing));
        when(driverExceptions.save(any(DriverException.class))).thenAnswer(inv -> inv.getArgument(0));

        var newStart = OffsetDateTime.of(2026, 9, 2, 9, 0, 0, 0, ZoneOffset.UTC);
        var newEnd = OffsetDateTime.of(2026, 9, 2, 17, 0, 0, 0, ZoneOffset.UTC);
        var updateCmd = new DriverExceptionUseCase.UpdateCommand(
                DriverExceptionType.DISCIPLINARY_SUSPENSION, newStart, newEnd, DriverExceptionStatus.ACTIVE, "New reason", "Updated remarks"
        );

        var updated = service.update(driverId, exceptionId, updateCmd, "editor");

        assertThat(updated.exceptionType()).isEqualTo(DriverExceptionType.DISCIPLINARY_SUSPENSION);
        assertThat(updated.startTime()).isEqualTo(newStart);
        assertThat(updated.endTime()).isEqualTo(newEnd);
        assertThat(updated.status()).isEqualTo(DriverExceptionStatus.ACTIVE);
        assertThat(updated.reason()).isEqualTo("New reason");
        assertThat(updated.updatedBy()).isEqualTo("editor");
        var captor = org.mockito.ArgumentCaptor.forClass(
                com.transportlogistics.app.notification.OperationalNotificationEvent.class);
        verify(notifications).publish(captor.capture());
        assertThat(captor.getValue().severity())
                .isEqualTo(com.transportlogistics.app.notification.OperationalNotificationEvent.Severity.CRITICAL);
        assertThat(captor.getValue().metadata()).containsEntry("transition", "BLOCKING_ACTIVE");
    }

    @Test
    void rejectsRescheduleWhenOverlappingActiveTripAssignmentExists_RuleC() {
        var exceptionId = UUID.randomUUID();
        var existing = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                DriverExceptionStatus.SCHEDULED, "Reason", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.findById(exceptionId)).thenReturn(Optional.of(existing));

        var newStart = OffsetDateTime.of(2026, 9, 2, 9, 0, 0, 0, ZoneOffset.UTC);
        var newEnd = OffsetDateTime.of(2026, 9, 2, 17, 0, 0, 0, ZoneOffset.UTC);
        var updateCmd = new DriverExceptionUseCase.UpdateCommand(
                null, newStart, newEnd, null, null, null
        );

        when(assignments.hasOverlap(eq(driverId), eq(newStart), eq(newEnd), isNull()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(driverId, exceptionId, updateCmd, "editor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active trip assignment");
    }

    @Test
    void rejectsInvalidStatusTransitionInUpdate() {
        var exceptionId = UUID.randomUUID();
        var existing = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                DriverExceptionStatus.ACTIVE, "Reason", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.findById(exceptionId)).thenReturn(Optional.of(existing));

        var updateCmd = new DriverExceptionUseCase.UpdateCommand(
                null, null, null, DriverExceptionStatus.SCHEDULED, null, null
        );

        assertThatThrownBy(() -> service.update(driverId, exceptionId, updateCmd, "editor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot transition driver exception");
    }

    @Test
    void cancelsDriverException() {
        var exceptionId = UUID.randomUUID();
        var existing = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                DriverExceptionStatus.SCHEDULED, "Reason", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.findById(exceptionId)).thenReturn(Optional.of(existing));
        when(driverExceptions.save(any(DriverException.class))).thenAnswer(inv -> inv.getArgument(0));

        var cancelled = service.cancel(driverId, exceptionId, "Trip urgent", "canceller");

        assertThat(cancelled.status()).isEqualTo(DriverExceptionStatus.CANCELLED);
        assertThat(cancelled.remarks()).contains("Cancelled: Trip urgent");
        assertThat(cancelled.updatedBy()).isEqualTo("canceller");
        verifyNoInteractions(notifications);
    }

    @Test
    void completesDriverException() {
        var exceptionId = UUID.randomUUID();
        var existing = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                DriverExceptionStatus.ACTIVE, "Reason", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.findById(exceptionId)).thenReturn(Optional.of(existing));
        when(driverExceptions.save(any(DriverException.class))).thenAnswer(inv -> inv.getArgument(0));

        var completed = service.complete(driverId, exceptionId, "Driver returned early", "completer");

        assertThat(completed.status()).isEqualTo(DriverExceptionStatus.COMPLETED);
        assertThat(completed.remarks()).contains("Completed: Driver returned early");
        assertThat(completed.updatedBy()).isEqualTo("completer");
        verifyNoInteractions(notifications);
    }

    @Test
    void notificationFailureDoesNotRollBackBlockingExceptionCreation() {
        var start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        var end = start.plusHours(8);
        when(driverExceptions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("listener failed")).when(notifications).publish(any());

        var created = service.create(driverId, new DriverExceptionUseCase.CreateCommand(
                DriverExceptionType.MEDICAL_EMERGENCY, start, end, "Emergency", null), "manager");

        assertThat(created.status()).isEqualTo(DriverExceptionStatus.SCHEDULED);
        verify(driverExceptions).save(any());
    }

    @Test
    void rejectsActionOnAlreadyCompletedOrCancelledException() {
        var exceptionId = UUID.randomUUID();
        var completed = new DriverException(
                exceptionId, driverId, DriverExceptionType.LEAVE,
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                DriverExceptionStatus.COMPLETED, null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(driverExceptions.findById(exceptionId)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.cancel(driverId, exceptionId, "Reason", "user"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot cancel a completed driver exception");

        assertThatThrownBy(() -> service.complete(driverId, exceptionId, "Remarks", "user"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Driver exception is already completed");
    }
}
