package com.transportlogistics.app.trip.domain.model;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/** Authoritative policy for Phase 1 trip lifecycle and assignment-state semantics. */
public final class TripLifecyclePolicy {
    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String APPROVED = "APPROVED";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String DISPATCHED = "DISPATCHED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CLOSED = "CLOSED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    private static final Set<String> CANCELLABLE = Set.of(
            DRAFT, SUBMITTED, REJECTED, APPROVED, ASSIGNED, DISPATCHED);

    public void validateOrder(Trip trip) {
        if (trip.originLocationId() == null) {
            throw validation("TRIP_NOT_SUBMITTABLE", "Trip origin is required");
        }
        if (trip.destinationLocationId() == null) {
            throw validation("TRIP_NOT_SUBMITTABLE", "Trip destination is required");
        }
        if (trip.originLocationId().equals(trip.destinationLocationId())) {
            throw validation("TRIP_NOT_SUBMITTABLE", "Trip origin and destination must be different");
        }
        if (trip.requestedStartTime() == null || trip.requestedEndTime() == null
                || !trip.requestedStartTime().isBefore(trip.requestedEndTime())) {
            throw validation("INVALID_TRIP_PERIOD",
                    "Trip requested start time must be before requested end time");
        }
        if (trip.priority() == null || trip.priority().isBlank()) {
            throw validation("TRIP_NOT_SUBMITTABLE", "Trip priority is required");
        }
        if (trip.requiredCapacityKg() != null && trip.requiredCapacityKg() < 0) {
            throw validation("TRIP_NOT_SUBMITTABLE", "Required capacity cannot be negative");
        }
        if (trip.passengerCount() != null && trip.passengerCount() < 0) {
            throw validation("TRIP_NOT_SUBMITTABLE", "Passenger count cannot be negative");
        }
    }

    public void requireEditable(Trip trip) {
        if (COMPLETED.equals(trip.status()) || CLOSED.equals(trip.status())) {
            throw conflict("TRIP_NOT_EDITABLE", "A " + trip.status() + " trip cannot be edited");
        }
    }

    public String statusAfterAssignment(Trip trip, UUID vehicleId, UUID driverId) {
        requireAssignmentAllowed(trip);
        return vehicleId != null && driverId != null ? ASSIGNED : APPROVED;
    }

    public void requireAssignmentAllowed(Trip trip) {
        if (!APPROVED.equals(trip.status()) && !ASSIGNED.equals(trip.status())) {
            throw conflict("INVALID_TRIP_TRANSITION",
                    "Resource assignment requires an APPROVED or ASSIGNED trip");
        }
    }

    public void requireRouteAssignmentAllowed(Trip trip) {
        if (!DRAFT.equals(trip.status()) && !APPROVED.equals(trip.status()) && !ASSIGNED.equals(trip.status())) {
            throw conflict("TRIP_ROUTE_NOT_ASSIGNABLE",
                    "Route assignment requires a DRAFT, APPROVED, or ASSIGNED trip");
        }
    }

    public void requireDispatchable(Trip trip) {
        if (!ASSIGNED.equals(trip.status())) {
            throw conflict("TRIP_NOT_DISPATCHABLE", "Dispatch requires an ASSIGNED trip");
        }
        if (trip.vehicleId() == null || trip.driverId() == null) {
            throw conflict("ASSIGNMENT_INCOMPLETE",
                    "Both a vehicle and driver assignment are required for dispatch");
        }
    }

    public void validateTransition(Trip trip, TripCommand command, String actor, OffsetDateTime now) {
        switch (command) {
            case TripCommand.Submit ignored -> {
                if (!DRAFT.equals(trip.status()) && !REJECTED.equals(trip.status())) {
                    throw conflict("TRIP_NOT_SUBMITTABLE", "Submit requires a DRAFT or REJECTED trip");
                }
                validateOrder(trip);
            }
            case TripCommand.Approve ignored -> {
                if (!SUBMITTED.equals(trip.status())) {
                    throw conflict("TRIP_NOT_APPROVABLE", "Approve requires a SUBMITTED trip");
                }
                if (actor == null || actor.isBlank()) {
                    throw conflict("TRIP_NOT_APPROVABLE", "An authenticated actor is required to approve a trip");
                }
            }
            case TripCommand.Reject reject -> {
                if (!SUBMITTED.equals(trip.status())) {
                    throw conflict("TRIP_NOT_REJECTABLE", "Reject requires a SUBMITTED trip");
                }
                requireReason(reject.reason(), "REJECTION_REASON_REQUIRED", "Rejection reason is required");
            }
            case TripCommand.Dispatch ignored -> requireDispatchable(trip);
            case TripCommand.Start start -> validateStart(trip, start);
            case TripCommand.Complete complete -> validateComplete(trip, complete, now);
            case TripCommand.Close ignored -> {
                if (!COMPLETED.equals(trip.status())) {
                    throw conflict("TRIP_NOT_CLOSABLE", "Close requires a COMPLETED trip");
                }
            }
            case TripCommand.Cancel cancel -> {
                if (!CANCELLABLE.contains(trip.status())) {
                    throw conflict("TRIP_NOT_CANCELLABLE",
                            "Trip cannot be cancelled from status " + trip.status());
                }
                requireReason(cancel.reason(), "CANCELLATION_REASON_REQUIRED", "Cancellation reason is required");
            }
        }
    }

    public String targetStatus(TripCommand command) {
        return switch (command) {
            case TripCommand.Submit ignored -> SUBMITTED;
            case TripCommand.Approve ignored -> APPROVED;
            case TripCommand.Reject ignored -> REJECTED;
            case TripCommand.Dispatch ignored -> DISPATCHED;
            case TripCommand.Start ignored -> IN_PROGRESS;
            case TripCommand.Complete ignored -> COMPLETED;
            case TripCommand.Close ignored -> CLOSED;
            case TripCommand.Cancel ignored -> CANCELLED;
        };
    }

    public String action(TripCommand command) {
        return switch (command) {
            case TripCommand.Submit ignored -> "TRIP_SUBMITTED";
            case TripCommand.Approve ignored -> "TRIP_APPROVED";
            case TripCommand.Reject ignored -> "TRIP_REJECTED";
            case TripCommand.Dispatch ignored -> "TRIP_DISPATCHED";
            case TripCommand.Start ignored -> "TRIP_STARTED";
            case TripCommand.Complete ignored -> "TRIP_COMPLETED";
            case TripCommand.Close ignored -> "TRIP_CLOSED";
            case TripCommand.Cancel ignored -> "TRIP_CANCELLED";
        };
    }

    public String details(TripCommand command) {
        return switch (command) {
            case TripCommand.Submit ignored -> "Trip submitted for approval";
            case TripCommand.Approve ignored -> "Trip approved";
            case TripCommand.Reject reject -> reject.reason().trim();
            case TripCommand.Dispatch ignored -> "Trip dispatched";
            case TripCommand.Start ignored -> "Trip started";
            case TripCommand.Complete complete -> textOrDefault(complete.remarks(), "Trip completed");
            case TripCommand.Close ignored -> "Trip closed";
            case TripCommand.Cancel cancel -> cancel.reason().trim();
        };
    }

    private void validateStart(Trip trip, TripCommand.Start start) {
        if (IN_PROGRESS.equals(trip.status()) || COMPLETED.equals(trip.status()) || CLOSED.equals(trip.status())) {
            throw conflict("TRIP_ALREADY_STARTED", "Trip has already been started");
        }
        if (!DISPATCHED.equals(trip.status())) {
            throw conflict("TRIP_NOT_STARTABLE", "Start requires a DISPATCHED trip");
        }
        requireReading(start.odometerKm(), "Start odometer is required", "Start odometer cannot be negative");
    }

    private void validateComplete(Trip trip, TripCommand.Complete complete, OffsetDateTime now) {
        if (!IN_PROGRESS.equals(trip.status())) {
            throw conflict("TRIP_NOT_COMPLETABLE", "Complete requires an IN_PROGRESS trip");
        }
        if (trip.actualStartTime() == null) {
            throw conflict("TRIP_NOT_COMPLETABLE", "Trip actual start time is required before completion");
        }
        if (now.isBefore(trip.actualStartTime())) {
            throw validation("INVALID_TRIP_PERIOD", "Trip actual end time cannot precede actual start time");
        }
        requireReading(complete.odometerKm(), "End odometer is required", "End odometer cannot be negative");
        if (trip.startOdometerKm() != null && complete.odometerKm() < trip.startOdometerKm()) {
            throw validation("INVALID_ODOMETER", "End odometer cannot be lower than start odometer");
        }
    }

    private void requireReading(Double value, String missingMessage, String negativeMessage) {
        if (value == null) {
            throw validation("INVALID_ODOMETER", missingMessage);
        }
        if (!Double.isFinite(value) || value < 0) {
            throw validation("INVALID_ODOMETER", negativeMessage);
        }
    }

    private void requireReason(String reason, String code, String message) {
        if (reason == null || reason.isBlank()) {
            throw validation(code, message);
        }
    }

    private String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private ConflictException conflict(String code, String message) {
        return new ConflictException(code, message);
    }

    private BusinessRuleException validation(String code, String message) {
        return new BusinessRuleException(code, message);
    }
}
