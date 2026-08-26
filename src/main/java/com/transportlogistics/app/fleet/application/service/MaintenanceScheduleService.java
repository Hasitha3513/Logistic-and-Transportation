package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.fleet.application.ports.in.MaintenanceScheduleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.MaintenanceScheduleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Transactional
public class MaintenanceScheduleService implements MaintenanceScheduleUseCase {

    private static final List<MaintenanceStatus> BLOCKING_STATUSES = List.of(
            MaintenanceStatus.SCHEDULED,
            MaintenanceStatus.IN_PROGRESS
    );

    private final MaintenanceScheduleRepository maintenanceSchedules;
    private final VehicleRepository vehicles;
    private final VehicleAllocationAvailability allocations;

    public MaintenanceScheduleService(MaintenanceScheduleRepository maintenanceSchedules,
                                      VehicleRepository vehicles,
                                      VehicleAllocationAvailability allocations) {
        this.maintenanceSchedules = maintenanceSchedules;
        this.vehicles = vehicles;
        this.allocations = allocations;
    }

    public MaintenanceScheduleService(MaintenanceScheduleRepository maintenanceSchedules,
                                      VehicleRepository vehicles) {
        this(maintenanceSchedules, vehicles, null);
    }

    @Override
    public MaintenanceSchedule create(UUID vehicleId, CreateCommand command, String actor) {
        vehicles.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));

        if (command.scheduledStart() == null || command.scheduledEnd() == null) {
            throw new IllegalArgumentException("Scheduled start and end are required");
        }
        if (!command.scheduledStart().isBefore(command.scheduledEnd())) {
            throw new IllegalArgumentException("Scheduled end must be strictly after scheduled start");
        }

        // Rule: no overlapping active maintenance
        if (maintenanceSchedules.hasOverlappingSchedule(vehicleId, command.scheduledStart(), command.scheduledEnd(), BLOCKING_STATUSES)) {
            throw new BusinessRuleException("MAINTENANCE_OVERLAP", "Vehicle already has an overlapping maintenance schedule in the requested period");
        }

        // Rule B: no overlapping active trip allocations
        if (allocations != null && allocations.hasOverlap(vehicleId, command.scheduledStart(), command.scheduledEnd(), null)) {
            throw new BusinessRuleException("TRIP_CONFLICT", "Vehicle has an active trip allocation during the scheduled maintenance window");
        }

        var now = OffsetDateTime.now();
        var schedule = new MaintenanceSchedule(
                UUID.randomUUID(),
                vehicleId,
                command.maintenanceType(),
                command.scheduledStart(),
                command.scheduledEnd(),
                MaintenanceStatus.SCHEDULED,
                command.description(),
                command.serviceProvider(),
                command.cost(),
                now,
                now,
                actor,
                actor
        );
        return maintenanceSchedules.save(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceSchedule get(UUID vehicleId, UUID scheduleId) {
        var schedule = maintenanceSchedules.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Maintenance schedule not found: " + scheduleId));
        if (!schedule.vehicleId().equals(vehicleId)) {
            throw new NotFoundException("Maintenance schedule " + scheduleId + " does not belong to vehicle " + vehicleId);
        }
        return schedule;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceSchedule> list(UUID vehicleId) {
        vehicles.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));
        return maintenanceSchedules.findByVehicleId(vehicleId);
    }

    @Override
    public MaintenanceSchedule update(UUID vehicleId, UUID scheduleId, UpdateCommand command, String actor) {
        vehicles.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));

        var existing = get(vehicleId, scheduleId);

        if (existing.status() == MaintenanceStatus.CANCELLED || existing.status() == MaintenanceStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot modify a maintenance schedule in status: " + existing.status());
        }

        var start = command.scheduledStart() != null ? command.scheduledStart() : existing.scheduledStart();
        var end = command.scheduledEnd() != null ? command.scheduledEnd() : existing.scheduledEnd();
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Scheduled end must be strictly after scheduled start");
        }

        // Validate lifecycle transition if status change requested
        var targetStatus = command.status() != null ? command.status() : existing.status();
        if (targetStatus != existing.status()) {
            validateTransition(existing.status(), targetStatus);
        }

        // Conflict checks if interval modified or if status becomes blocking
        if (BLOCKING_STATUSES.contains(targetStatus)) {
            if (maintenanceSchedules.hasOverlappingScheduleExcluding(vehicleId, start, end, BLOCKING_STATUSES, scheduleId)) {
                throw new BusinessRuleException("MAINTENANCE_OVERLAP", "Vehicle already has an overlapping maintenance schedule in the requested period");
            }
            // Rule C: reschedule conflict with active trip allocation
            if (allocations != null && allocations.hasOverlap(vehicleId, start, end, null)) {
                throw new BusinessRuleException("TRIP_CONFLICT", "Vehicle has an active trip allocation during the rescheduled maintenance window");
            }
        }

        var type = command.maintenanceType() != null ? command.maintenanceType() : existing.maintenanceType();
        var desc = command.description() != null ? command.description() : existing.description();
        var provider = command.serviceProvider() != null ? command.serviceProvider() : existing.serviceProvider();
        var cost = command.cost() != null ? command.cost() : existing.cost();

        var updated = new MaintenanceSchedule(
                existing.id(),
                existing.vehicleId(),
                type,
                start,
                end,
                targetStatus,
                desc,
                provider,
                cost,
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return maintenanceSchedules.save(updated);
    }

    @Override
    public MaintenanceSchedule cancel(UUID vehicleId, UUID scheduleId, String reason, String actor) {
        vehicles.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));

        var existing = get(vehicleId, scheduleId);

        if (existing.status() == MaintenanceStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATE", "Maintenance schedule is already cancelled");
        }
        if (existing.status() == MaintenanceStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot cancel a completed maintenance schedule");
        }

        var description = existing.description();
        if (reason != null && !reason.trim().isEmpty()) {
            description = (description != null ? description + " | " : "") + "Cancelled: " + reason.trim();
        }

        var cancelled = new MaintenanceSchedule(
                existing.id(),
                existing.vehicleId(),
                existing.maintenanceType(),
                existing.scheduledStart(),
                existing.scheduledEnd(),
                MaintenanceStatus.CANCELLED,
                description,
                existing.serviceProvider(),
                existing.cost(),
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return maintenanceSchedules.save(cancelled);
    }

    @Override
    public MaintenanceSchedule complete(UUID vehicleId, UUID scheduleId, String remarks, String actor) {
        vehicles.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));

        var existing = get(vehicleId, scheduleId);

        if (existing.status() == MaintenanceStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Maintenance schedule is already completed");
        }
        if (existing.status() == MaintenanceStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot complete a cancelled maintenance schedule");
        }

        var description = existing.description();
        if (remarks != null && !remarks.trim().isEmpty()) {
            description = (description != null ? description + " | " : "") + "Completed: " + remarks.trim();
        }

        var completed = new MaintenanceSchedule(
                existing.id(),
                existing.vehicleId(),
                existing.maintenanceType(),
                existing.scheduledStart(),
                existing.scheduledEnd(),
                MaintenanceStatus.COMPLETED,
                description,
                existing.serviceProvider(),
                existing.cost(),
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return maintenanceSchedules.save(completed);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOverlappingSchedule(UUID vehicleId, OffsetDateTime from, OffsetDateTime to) {
        return maintenanceSchedules.hasOverlappingSchedule(vehicleId, from, to, BLOCKING_STATUSES);
    }

    private void validateTransition(MaintenanceStatus current, MaintenanceStatus target) {
        boolean valid = switch (current) {
            case SCHEDULED -> target == MaintenanceStatus.IN_PROGRESS
                    || target == MaintenanceStatus.COMPLETED
                    || target == MaintenanceStatus.CANCELLED;
            case IN_PROGRESS -> target == MaintenanceStatus.COMPLETED
                    || target == MaintenanceStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessRuleException("INVALID_TRANSITION",
                    "Cannot transition maintenance schedule from " + current + " to " + target);
        }
    }
}
