package com.transportlogistics.app.trip.domain.model;

import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripLifecyclePolicyTest {
    private final TripLifecyclePolicy policy = new TripLifecyclePolicy();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-14T12:00:00Z");

    @Test
    void allowsOnlyTheDefinedAuthorizationTransitions() {
        policy.validateTransition(trip("DRAFT"), new TripCommand.Submit(), "requester", now);
        policy.validateTransition(trip("REJECTED"), new TripCommand.Submit(), "requester", now);
        policy.validateTransition(trip("SUBMITTED"), new TripCommand.Approve(), "approver", now);
        policy.validateTransition(trip("SUBMITTED"), new TripCommand.Reject("Insufficient details"), "approver", now);

        var error = assertThrows(ConflictException.class,
                () -> policy.validateTransition(trip("DRAFT"), new TripCommand.Approve(), "approver", now));
        assertEquals("TRIP_NOT_APPROVABLE", error.code());
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateTransition(trip("SUBMITTED"), new TripCommand.Reject(" "), "approver", now));
    }

    @Test
    void assignmentIsCompleteOnlyWhenBothResourcesArePresent() {
        var approved = trip("APPROVED");
        assertEquals("APPROVED", policy.statusAfterAssignment(approved, UUID.randomUUID(), null));
        assertEquals("APPROVED", policy.statusAfterAssignment(approved, null, UUID.randomUUID()));
        assertEquals("ASSIGNED", policy.statusAfterAssignment(approved, UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void startCompleteCloseAndCancelValidateStateAndExecutionValues() {
        policy.validateTransition(trip("DISPATCHED"), new TripCommand.Start(100.0), "driver", now);
        policy.validateTransition(trip("IN_PROGRESS", now.minusHours(1), 100.0),
                new TripCommand.Complete(110.0, "Delivered"), "driver", now);
        policy.validateTransition(trip("COMPLETED"), new TripCommand.Close(), "closer", now);

        assertThrows(IllegalArgumentException.class,
                () -> policy.validateTransition(trip("DISPATCHED"), new TripCommand.Start(-1.0), "driver", now));
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateTransition(trip("IN_PROGRESS", now.minusHours(1), 100.0),
                        new TripCommand.Complete(99.0, null), "driver", now));
        var closed = assertThrows(ConflictException.class,
                () -> policy.validateTransition(trip("CLOSED"), new TripCommand.Cancel("No"), "dispatcher", now));
        assertEquals("TRIP_NOT_CANCELLABLE", closed.code());
        var inProgress = assertThrows(ConflictException.class,
                () -> policy.validateTransition(trip("IN_PROGRESS"), new TripCommand.Cancel("Breakdown"),
                        "dispatcher", now));
        assertEquals("TRIP_NOT_CANCELLABLE", inProgress.code());
    }

    @Test
    void mandatoryTripOrderFieldsAreValidatedAtSubmission() {
        var invalid = new Trip(UUID.randomUUID(), "TRIP-1", null, null, null, null, "NORMAL", "DRAFT",
                UUID.randomUUID(), UUID.randomUUID(), now.plusHours(2), now.plusHours(1), null, -1.0,
                null, -1, null, null, null, null, null, null, null, null, null, now, now);

        assertThrows(IllegalArgumentException.class,
                () -> policy.validateTransition(invalid, new TripCommand.Submit(), "requester", now));
    }

    private Trip trip(String status) {
        return trip(status, null, null);
    }

    private Trip trip(String status, OffsetDateTime actualStart, Double startOdometer) {
        return new Trip(UUID.randomUUID(), "TRIP-1", null, null, null, null, "NORMAL", status,
                UUID.randomUUID(), UUID.randomUUID(), now.plusDays(1), now.plusDays(2), null, null,
                null, null, null, null, null, null, actualStart, null, startOdometer, null, null, now, now);
    }
}
