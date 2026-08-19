package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.application.ports.in.MaintenanceScheduleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.MaintenanceScheduleRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MaintenanceScheduleServiceTest {

    private MaintenanceScheduleRepository maintenanceSchedules;
    private VehicleRepository vehicles;
    private VehicleAllocationAvailability allocations;
    private MaintenanceScheduleService service;

    private final UUID vehicleId = UUID.randomUUID();
    private final Vehicle vehicle = new Vehicle(
            vehicleId, "WP-CAB-1234", "12345", "Engine1", UUID.randomUUID(), UUID.randomUUID(),
            "Maker", "Model", 2025, "OWNED", "AVAILABLE", 1000.0, 50.0, 5000.0, true
    );

    @BeforeEach
    void setUp() {
        maintenanceSchedules = mock(MaintenanceScheduleRepository.class);
        vehicles = mock(VehicleRepository.class);
        allocations = mock(VehicleAllocationAvailability.class);
        service = new MaintenanceScheduleService(maintenanceSchedules, vehicles, allocations);

        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicles.findByIdForUpdate(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void createsValidMaintenanceSchedule() {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var command = new MaintenanceScheduleUseCase.CreateCommand(
                "Preventive 50k km Service", start, end, "Full service", "Dealer", new BigDecimal("350.00")
        );

        when(maintenanceSchedules.save(any(MaintenanceSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var created = service.create(vehicleId, command, "fleet.manager");

        assertThat(created).isNotNull();
        assertThat(created.vehicleId()).isEqualTo(vehicleId);
        assertThat(created.maintenanceType()).isEqualTo("Preventive 50k km Service");
        assertThat(created.status()).isEqualTo(MaintenanceStatus.SCHEDULED);
        assertThat(created.createdBy()).isEqualTo("fleet.manager");
        verify(maintenanceSchedules, times(1)).save(any(MaintenanceSchedule.class));
    }

    @Test
    void rejectsCreationWhenVehicleNotFound() {
        var missingVehicleId = UUID.randomUUID();
        when(vehicles.findById(missingVehicleId)).thenReturn(Optional.empty());
        when(vehicles.findByIdForUpdate(missingVehicleId)).thenReturn(Optional.empty());

        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var command = new MaintenanceScheduleUseCase.CreateCommand("Service", start, end, null, null, null);

        assertThatThrownBy(() -> service.create(missingVehicleId, command, "actor"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Vehicle not found");
    }

    @Test
    void rejectsCreationWhenDatesInvalid() {
        var start = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var command = new MaintenanceScheduleUseCase.CreateCommand("Service", start, end, null, null, null);

        assertThatThrownBy(() -> service.create(vehicleId, command, "actor"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCreationWhenOverlappingActiveMaintenanceScheduleExists() {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var command = new MaintenanceScheduleUseCase.CreateCommand("Service", start, end, null, null, null);

        when(maintenanceSchedules.hasOverlappingSchedule(eq(vehicleId), eq(start), eq(end), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(vehicleId, command, "actor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has an overlapping maintenance schedule");
    }

    @Test
    void rejectsCreationWhenOverlappingActiveTripExists_RuleB() {
        var start = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC);
        var end = OffsetDateTime.of(2026, 9, 1, 16, 0, 0, 0, ZoneOffset.UTC);
        var command = new MaintenanceScheduleUseCase.CreateCommand("Service", start, end, null, null, null);

        when(allocations.hasOverlap(eq(vehicleId), eq(start), eq(end), isNull()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(vehicleId, command, "actor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active trip allocation");
    }

    @Test
    void retrievesScheduleSuccessfully() {
        var scheduleId = UUID.randomUUID();
        var schedule = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service",
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC).plusHours(4),
                MaintenanceStatus.SCHEDULED, null, null, null, OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.findById(scheduleId)).thenReturn(Optional.of(schedule));

        var result = service.get(vehicleId, scheduleId);
        assertThat(result).isEqualTo(schedule);
    }

    @Test
    void rejectsGetWhenVehicleMismatch() {
        var scheduleId = UUID.randomUUID();
        var schedule = new MaintenanceSchedule(
                scheduleId, UUID.randomUUID(), "Service",
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC).plusHours(4),
                MaintenanceStatus.SCHEDULED, null, null, null, OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.findById(scheduleId)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.get(vehicleId, scheduleId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("does not belong to vehicle");
    }

    @Test
    void updatesAndReschedulesSuccessfully() {
        var scheduleId = UUID.randomUUID();
        var existing = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service",
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                MaintenanceStatus.SCHEDULED, "Old desc", null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.findById(scheduleId)).thenReturn(Optional.of(existing));
        when(maintenanceSchedules.save(any(MaintenanceSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var newStart = OffsetDateTime.of(2026, 9, 2, 9, 0, 0, 0, ZoneOffset.UTC);
        var newEnd = OffsetDateTime.of(2026, 9, 2, 17, 0, 0, 0, ZoneOffset.UTC);
        var updateCmd = new MaintenanceScheduleUseCase.UpdateCommand(
                "Major Service", newStart, newEnd, MaintenanceStatus.SCHEDULED, "New desc", "New Provider", new BigDecimal("500.00")
        );

        var updated = service.update(vehicleId, scheduleId, updateCmd, "editor");

        assertThat(updated.maintenanceType()).isEqualTo("Major Service");
        assertThat(updated.scheduledStart()).isEqualTo(newStart);
        assertThat(updated.scheduledEnd()).isEqualTo(newEnd);
        assertThat(updated.description()).isEqualTo("New desc");
        assertThat(updated.updatedBy()).isEqualTo("editor");
    }

    @Test
    void rejectsRescheduleWhenOverlappingActiveTripExists_RuleC() {
        var scheduleId = UUID.randomUUID();
        var existing = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service",
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                MaintenanceStatus.SCHEDULED, "Desc", null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.findById(scheduleId)).thenReturn(Optional.of(existing));

        var newStart = OffsetDateTime.of(2026, 9, 2, 9, 0, 0, 0, ZoneOffset.UTC);
        var newEnd = OffsetDateTime.of(2026, 9, 2, 17, 0, 0, 0, ZoneOffset.UTC);
        var updateCmd = new MaintenanceScheduleUseCase.UpdateCommand(
                null, newStart, newEnd, null, null, null, null
        );

        when(allocations.hasOverlap(eq(vehicleId), eq(newStart), eq(newEnd), isNull()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(vehicleId, scheduleId, updateCmd, "editor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active trip allocation");
    }

    @Test
    void rejectsInvalidStatusTransitionInUpdate() {
        var scheduleId = UUID.randomUUID();
        var existing = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service",
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                MaintenanceStatus.IN_PROGRESS, "Desc", null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.findById(scheduleId)).thenReturn(Optional.of(existing));

        var updateCmd = new MaintenanceScheduleUseCase.UpdateCommand(
                null, null, null, MaintenanceStatus.SCHEDULED, null, null, null
        );

        assertThatThrownBy(() -> service.update(vehicleId, scheduleId, updateCmd, "editor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot transition maintenance schedule");
    }

    @Test
    void cancelsMaintenanceSchedule() {
        var scheduleId = UUID.randomUUID();
        var existing = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service",
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                MaintenanceStatus.SCHEDULED, "Desc", null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.findById(scheduleId)).thenReturn(Optional.of(existing));
        when(maintenanceSchedules.save(any(MaintenanceSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var cancelled = service.cancel(vehicleId, scheduleId, "Parts delayed", "canceller");

        assertThat(cancelled.status()).isEqualTo(MaintenanceStatus.CANCELLED);
        assertThat(cancelled.description()).contains("Cancelled: Parts delayed");
        assertThat(cancelled.updatedBy()).isEqualTo("canceller");
    }

    @Test
    void completesMaintenanceSchedule() {
        var scheduleId = UUID.randomUUID();
        var existing = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service",
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                MaintenanceStatus.IN_PROGRESS, "Desc", null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.findById(scheduleId)).thenReturn(Optional.of(existing));
        when(maintenanceSchedules.save(any(MaintenanceSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var completed = service.complete(vehicleId, scheduleId, "All tests passed", "completer");

        assertThat(completed.status()).isEqualTo(MaintenanceStatus.COMPLETED);
        assertThat(completed.description()).contains("Completed: All tests passed");
        assertThat(completed.updatedBy()).isEqualTo("completer");
    }

    @Test
    void rejectsActionOnAlreadyCompletedOrCancelledSchedule() {
        var scheduleId = UUID.randomUUID();
        var completed = new MaintenanceSchedule(
                scheduleId, vehicleId, "Service",
                OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                MaintenanceStatus.COMPLETED, null, null, null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );

        when(maintenanceSchedules.findById(scheduleId)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.cancel(vehicleId, scheduleId, "Reason", "user"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot cancel a completed maintenance schedule");

        assertThatThrownBy(() -> service.complete(vehicleId, scheduleId, "Remarks", "user"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Maintenance schedule is already completed");
    }
}
