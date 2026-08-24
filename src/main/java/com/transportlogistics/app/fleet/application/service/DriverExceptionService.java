package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.DriverAssignmentAvailability;
import com.transportlogistics.app.fleet.application.ports.in.DriverExceptionUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverExceptionRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.FleetOperationalNotificationPublisher;
import com.transportlogistics.app.fleet.domain.model.DriverException;
import com.transportlogistics.app.fleet.domain.model.DriverExceptionStatus;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Transactional
public class DriverExceptionService implements DriverExceptionUseCase {
    private static final Logger log = LoggerFactory.getLogger(DriverExceptionService.class);

    private static final List<DriverExceptionStatus> BLOCKING_STATUSES = List.of(
            DriverExceptionStatus.SCHEDULED,
            DriverExceptionStatus.ACTIVE
    );

    private final DriverExceptionRepository driverExceptions;
    private final DriverRepository drivers;
    private final DriverAssignmentAvailability assignments;
    private final FleetOperationalNotificationPublisher notifications;

    public DriverExceptionService(DriverExceptionRepository driverExceptions,
                                  DriverRepository drivers,
                                  DriverAssignmentAvailability assignments,
                                  FleetOperationalNotificationPublisher notifications) {
        this.driverExceptions = driverExceptions;
        this.drivers = drivers;
        this.assignments = assignments;
        this.notifications = notifications;
    }

    public DriverExceptionService(DriverExceptionRepository driverExceptions,
                                  DriverRepository drivers,
                                  DriverAssignmentAvailability assignments) {
        this(driverExceptions, drivers, assignments, event -> {});
    }

    public DriverExceptionService(DriverExceptionRepository driverExceptions,
                                  DriverRepository drivers) {
        this(driverExceptions, drivers, null);
    }

    @Override
    public DriverException create(UUID driverId, CreateCommand command, String actor) {
        var driver = drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        if (command.exceptionType() == null) {
            throw new IllegalArgumentException("Driver exception type is required");
        }
        if (command.startTime() == null || command.endTime() == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!command.startTime().isBefore(command.endTime())) {
            throw new IllegalArgumentException("End time must be strictly after start time");
        }

        // Rule: check for overlapping active/scheduled driver exceptions
        if (driverExceptions.hasOverlappingException(driverId, command.startTime(), command.endTime(), BLOCKING_STATUSES)) {
            throw new BusinessRuleException("DRIVER_EXCEPTION_OVERLAP",
                    "Driver already has a scheduled exception or leave in the requested period");
        }

        // Rule B: check for overlapping active trip assignments
        if (assignments != null && assignments.hasOverlap(driverId, command.startTime(), command.endTime(), null)) {
            throw new BusinessRuleException("TRIP_CONFLICT",
                    "Driver has an active trip assignment during the requested exception period");
        }

        var now = OffsetDateTime.now();
        var exception = new DriverException(
                UUID.randomUUID(),
                driverId,
                command.exceptionType(),
                command.startTime(),
                command.endTime(),
                DriverExceptionStatus.SCHEDULED,
                command.reason(),
                command.remarks(),
                now,
                now,
                actor,
                actor
        );
        var saved = driverExceptions.save(exception);
        publishSafely(FleetOperationalNotificationEvents.driverException(saved, driver,
                "BLOCKING_" + saved.status().name(), now));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public DriverException get(UUID driverId, UUID exceptionId) {
        var exception = driverExceptions.findById(exceptionId)
                .orElseThrow(() -> new NotFoundException("Driver exception not found: " + exceptionId));
        if (!exception.driverId().equals(driverId)) {
            throw new NotFoundException("Driver exception " + exceptionId + " does not belong to driver " + driverId);
        }
        return exception;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverException> list(UUID driverId) {
        drivers.findById(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));
        return driverExceptions.findByDriverId(driverId);
    }

    @Override
    public DriverException update(UUID driverId, UUID exceptionId, UpdateCommand command, String actor) {
        var driver = drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        var existing = get(driverId, exceptionId);

        if (existing.status() == DriverExceptionStatus.CANCELLED || existing.status() == DriverExceptionStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot modify a driver exception in status: " + existing.status());
        }

        var start = command.startTime() != null ? command.startTime() : existing.startTime();
        var end = command.endTime() != null ? command.endTime() : existing.endTime();
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("End time must be strictly after start time");
        }

        var targetStatus = command.status() != null ? command.status() : existing.status();
        if (targetStatus != existing.status()) {
            validateTransition(existing.status(), targetStatus);
        }

        // Conflict checks if interval modified or if status is blocking
        if (BLOCKING_STATUSES.contains(targetStatus)) {
            if (driverExceptions.hasOverlappingExceptionExcluding(driverId, start, end, BLOCKING_STATUSES, exceptionId)) {
                throw new BusinessRuleException("DRIVER_EXCEPTION_OVERLAP",
                        "Driver already has a scheduled exception or leave in the requested period");
            }
            // Rule C: reschedule conflict with active trip assignment
            if (assignments != null && assignments.hasOverlap(driverId, start, end, null)) {
                throw new BusinessRuleException("TRIP_CONFLICT",
                        "Driver has an active trip assignment during the rescheduled exception period");
            }
        }

        var type = command.exceptionType() != null ? command.exceptionType() : existing.exceptionType();
        var reason = command.reason() != null ? command.reason() : existing.reason();
        var remarks = command.remarks() != null ? command.remarks() : existing.remarks();

        var updated = new DriverException(
                existing.id(),
                existing.driverId(),
                type,
                start,
                end,
                targetStatus,
                reason,
                remarks,
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        var saved = driverExceptions.save(updated);
        if (saved.status() == DriverExceptionStatus.ACTIVE && existing.status() != DriverExceptionStatus.ACTIVE) {
            publishSafely(FleetOperationalNotificationEvents.driverException(saved, driver,
                    "BLOCKING_ACTIVE", saved.updatedAt()));
        }
        return saved;
    }

    @Override
    public DriverException cancel(UUID driverId, UUID exceptionId, String remarks, String actor) {
        drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        var existing = get(driverId, exceptionId);

        if (existing.status() == DriverExceptionStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATE", "Driver exception is already cancelled");
        }
        if (existing.status() == DriverExceptionStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot cancel a completed driver exception");
        }

        var combinedRemarks = existing.remarks();
        if (remarks != null && !remarks.trim().isEmpty()) {
            combinedRemarks = (combinedRemarks != null ? combinedRemarks + " | " : "") + "Cancelled: " + remarks.trim();
        }

        var cancelled = new DriverException(
                existing.id(),
                existing.driverId(),
                existing.exceptionType(),
                existing.startTime(),
                existing.endTime(),
                DriverExceptionStatus.CANCELLED,
                existing.reason(),
                combinedRemarks,
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return driverExceptions.save(cancelled);
    }

    @Override
    public DriverException complete(UUID driverId, UUID exceptionId, String remarks, String actor) {
        drivers.findByIdForUpdate(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + driverId));

        var existing = get(driverId, exceptionId);

        if (existing.status() == DriverExceptionStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE", "Driver exception is already completed");
        }
        if (existing.status() == DriverExceptionStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATE", "Cannot complete a cancelled driver exception");
        }

        var combinedRemarks = existing.remarks();
        if (remarks != null && !remarks.trim().isEmpty()) {
            combinedRemarks = (combinedRemarks != null ? combinedRemarks + " | " : "") + "Completed: " + remarks.trim();
        }

        var completed = new DriverException(
                existing.id(),
                existing.driverId(),
                existing.exceptionType(),
                existing.startTime(),
                existing.endTime(),
                DriverExceptionStatus.COMPLETED,
                existing.reason(),
                combinedRemarks,
                existing.createdAt(),
                OffsetDateTime.now(),
                existing.createdBy(),
                actor
        );
        return driverExceptions.save(completed);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOverlappingException(UUID driverId, OffsetDateTime from, OffsetDateTime to) {
        return driverExceptions.hasOverlappingException(driverId, from, to, BLOCKING_STATUSES);
    }

    private void validateTransition(DriverExceptionStatus current, DriverExceptionStatus target) {
        boolean valid = switch (current) {
            case SCHEDULED -> target == DriverExceptionStatus.ACTIVE
                    || target == DriverExceptionStatus.COMPLETED
                    || target == DriverExceptionStatus.CANCELLED;
            case ACTIVE -> target == DriverExceptionStatus.COMPLETED
                    || target == DriverExceptionStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessRuleException("INVALID_TRANSITION",
                    "Cannot transition driver exception from " + current + " to " + target);
        }
    }

    private void publishSafely(com.transportlogistics.app.notification.OperationalNotificationEvent event) {
        try {
            notifications.publish(event);
        } catch (RuntimeException exception) {
            log.error("Driver exception notification publication failed for event {}", event.eventId(), exception);
        }
    }
}
