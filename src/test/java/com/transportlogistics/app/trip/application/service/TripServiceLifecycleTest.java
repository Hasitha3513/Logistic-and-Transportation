package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.*;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripServiceLifecycleTest {
    private static final Instant INSTANT = Instant.parse("2026-08-14T12:00:00Z");
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC);

    private TripRepository repository;
    private VehicleEligibilityPort vehicles;
    private DriverEligibilityPort drivers;
    private TripHistoryRepository history;
    private CountingTransaction transactions;
    private TripService service;
    private AtomicReference<Trip> persisted;
    private ArrayList<TripHistoryEntry> audit;

    @BeforeEach
    void setUp() {
        repository = mock(TripRepository.class);
        vehicles = mock(VehicleEligibilityPort.class);
        drivers = mock(DriverEligibilityPort.class);
        history = mock(TripHistoryRepository.class);
        transactions = new CountingTransaction();
        persisted = new AtomicReference<>();
        audit = new ArrayList<>();
        when(repository.findById(any())).thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
        when(repository.save(any())).thenAnswer(invocation -> {
            var saved = invocation.<Trip>getArgument(0);
            persisted.set(saved);
            return saved;
        });
        when(history.save(any())).thenAnswer(invocation -> {
            var entry = invocation.<TripHistoryEntry>getArgument(0);
            audit.add(entry);
            return entry;
        });
        service = new TripService(repository, vehicles, drivers, history, transactions,
                mock(TripDispatchRepository.class), Clock.fixed(INSTANT, ZoneOffset.UTC));
    }

    @Test
    void draftToSubmittedSucceedsTransactionallyAndWritesCompleteHistory() {
        given(trip("DRAFT"));

        var result = service.transition(persisted.get().id(), new TripCommand.Submit(), "requester");

        assertEquals("SUBMITTED", result.status());
        assertHistory("DRAFT", "SUBMITTED", "TRIP_SUBMITTED", "requester");
        assertEquals(1, transactions.executions);
    }

    @Test
    void applicationServiceOwnsTripCreationDefaultsAndTime() {
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        var created = service.create(new TripUseCase.CreateCommand(null, null, null, null, null, origin,
                destination, NOW.plusDays(1), NOW.plusDays(2), null, 100.0, "Cargo", 0, null, null));

        assertNotNull(created.id());
        assertTrue(created.tripNumber().startsWith("TRIP-"));
        assertEquals("NORMAL", created.priority());
        assertEquals("DRAFT", created.status());
        assertEquals(NOW, created.createdAt());
        assertEquals(NOW, created.updatedAt());
    }

    @Test
    void draftToApprovedFailsAndLeavesTripUnchanged() {
        var original = trip("DRAFT");
        given(original);

        var error = assertThrows(ConflictException.class,
                () -> service.transition(original.id(), new TripCommand.Approve(), "approver"));

        assertEquals("TRIP_NOT_APPROVABLE", error.code());
        assertSame(original, persisted.get());
        verify(repository, never()).save(any());
        assertTrue(audit.isEmpty());
    }

    @Test
    void submittedCanBeApprovedByAuthenticatedActor() {
        given(trip("SUBMITTED"));

        var result = service.transition(persisted.get().id(), new TripCommand.Approve(), "approver");

        assertEquals("APPROVED", result.status());
        assertHistory("SUBMITTED", "APPROVED", "TRIP_APPROVED", "approver");
        given(trip("SUBMITTED"));
        assertThrows(ConflictException.class,
                () -> service.transition(persisted.get().id(), new TripCommand.Approve(), null));
    }

    @Test
    void submittedCanBeRejectedWithReasonAndThenResubmitted() {
        given(trip("SUBMITTED"));

        assertThrows(IllegalArgumentException.class,
                () -> service.transition(persisted.get().id(), new TripCommand.Reject(" "), "approver"));
        assertEquals("SUBMITTED", persisted.get().status());
        var rejected = service.transition(persisted.get().id(), new TripCommand.Reject("Missing route"), "approver");
        assertEquals("REJECTED", rejected.status());
        assertEquals("Missing route", rejected.completionRemarks());
        var resubmitted = service.transition(rejected.id(), new TripCommand.Submit(), "requester");

        assertEquals("SUBMITTED", resubmitted.status());
        assertNull(resubmitted.completionRemarks());
        assertEquals(2, audit.size());
        assertEquals("Missing route", audit.getFirst().details());
        assertHistory("REJECTED", "SUBMITTED", "TRIP_SUBMITTED", "requester");
    }

    @Test
    void vehicleOnlyAndDriverOnlyAssignmentsRemainApproved() {
        var vehicleId = UUID.randomUUID();
        given(trip("APPROVED"));
        assertEquals("APPROVED", service.assignVehicle(persisted.get().id(), vehicleId, "allocator").status());

        given(trip("APPROVED"));
        var driverId = UUID.randomUUID();
        assertEquals("APPROVED",
                service.assignDriver(persisted.get().id(), driverId, "B", "allocator").status());
    }

    @Test
    void tripBecomesAssignedOnlyAfterBothEligibleAssignmentsExist() {
        given(trip("APPROVED"));
        var vehicleId = UUID.randomUUID();
        var driverId = UUID.randomUUID();

        var partial = service.assignVehicle(persisted.get().id(), vehicleId, "allocator");
        var complete = service.assignDriver(partial.id(), driverId, "B", "allocator");

        assertEquals("APPROVED", partial.status());
        assertEquals("ASSIGNED", complete.status());
        assertEquals(vehicleId, complete.vehicleId());
        assertEquals(driverId, complete.driverId());
        assertEquals("APPROVED", audit.getFirst().toStatus());
        assertEquals("ASSIGNED", audit.getLast().toStatus());
        assertEquals(2, transactions.executions);
    }

    @Test
    void unassigningEitherResourceRecalculatesAssignedTripToApproved() {
        var assigned = trip("ASSIGNED", UUID.randomUUID(), UUID.randomUUID(), null, null);
        given(assigned);
        var withoutVehicle = service.unassignVehicle(assigned.id(), "allocator");
        assertEquals("APPROVED", withoutVehicle.status());
        assertNull(withoutVehicle.vehicleId());
        assertNotNull(withoutVehicle.driverId());
        assertHistory("ASSIGNED", "APPROVED", "VEHICLE_UNASSIGNED", "allocator");

        given(assigned);
        audit.clear();
        var withoutDriver = service.unassignDriver(assigned.id(), "allocator");
        assertEquals("APPROVED", withoutDriver.status());
        assertNotNull(withoutDriver.vehicleId());
        assertNull(withoutDriver.driverId());
        assertHistory("ASSIGNED", "APPROVED", "DRIVER_UNASSIGNED", "allocator");
    }

    @Test
    void dispatchedTripStartsOnceWithActualTimeOdometerAndAudit() {
        var dispatched = trip("DISPATCHED", UUID.randomUUID(), UUID.randomUUID(), null, null);
        given(dispatched);

        var started = service.transition(dispatched.id(), new TripCommand.Start(1250.0), "driver");

        assertEquals("IN_PROGRESS", started.status());
        assertEquals(NOW, started.actualStartTime());
        assertEquals(1250.0, started.startOdometerKm());
        assertHistory("DISPATCHED", "IN_PROGRESS", "TRIP_STARTED", "driver");
        var saveCount = mockingDetails(repository).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save")).count();
        var duplicate = assertThrows(ConflictException.class,
                () -> service.transition(started.id(), new TripCommand.Start(1251.0), "driver"));
        assertEquals("TRIP_ALREADY_STARTED", duplicate.code());
        assertEquals(saveCount, mockingDetails(repository).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("save")).count());
    }

    @Test
    void approvedTripCannotStartAndRejectedCommandDoesNotPersist() {
        var approved = trip("APPROVED");
        given(approved);

        var error = assertThrows(ConflictException.class,
                () -> service.transition(approved.id(), new TripCommand.Start(10.0), "driver"));

        assertEquals("TRIP_NOT_STARTABLE", error.code());
        assertSame(approved, persisted.get());
        verify(repository, never()).save(any());
        assertTrue(audit.isEmpty());
    }

    @Test
    void dispatchRejectsUnapprovedStateAndIncompleteAssignmentsWithSpecificCodes() {
        var approved = trip("APPROVED");
        given(approved);
        var notDispatchable = assertThrows(ConflictException.class,
                () -> service.dispatch(approved.id(), "dispatcher", null));
        assertEquals("TRIP_NOT_DISPATCHABLE", notDispatchable.code());

        var incomplete = trip("ASSIGNED", UUID.randomUUID(), null, null, null);
        given(incomplete);
        var assignmentError = assertThrows(ConflictException.class,
                () -> service.dispatch(incomplete.id(), "dispatcher", null));
        assertEquals("ASSIGNMENT_INCOMPLETE", assignmentError.code());
        verify(repository, never()).save(any());
        assertTrue(audit.isEmpty());
    }

    @Test
    void inProgressTripCompletesThenClosesWithHistoryForBothTransitions() {
        var inProgress = trip("IN_PROGRESS", UUID.randomUUID(), UUID.randomUUID(), NOW.minusHours(2), 1000.0);
        given(inProgress);

        var completed = service.transition(inProgress.id(), new TripCommand.Complete(1050.0, "Delivered"), "driver");
        var closed = service.transition(completed.id(), new TripCommand.Close(), "supervisor");

        assertEquals("COMPLETED", completed.status());
        assertEquals(NOW, completed.actualEndTime());
        assertEquals(1050.0, completed.endOdometerKm());
        assertEquals("CLOSED", closed.status());
        assertEquals(2, audit.size());
        assertEquals("TRIP_COMPLETED", audit.getFirst().action());
        assertEquals("TRIP_CLOSED", audit.getLast().action());
        assertEquals("supervisor", audit.getLast().actor());
        assertEquals(2, transactions.executions);
    }

    @Test
    void invalidOdometerAndEndTimeValuesLeaveTripUnchanged() {
        var dispatched = trip("DISPATCHED", UUID.randomUUID(), UUID.randomUUID(), null, null);
        given(dispatched);
        assertThrows(IllegalArgumentException.class,
                () -> service.transition(dispatched.id(), new TripCommand.Start(-1.0), "driver"));
        assertSame(dispatched, persisted.get());

        var inProgress = trip("IN_PROGRESS", UUID.randomUUID(), UUID.randomUUID(), NOW.minusHours(1), 100.0);
        given(inProgress);
        assertThrows(IllegalArgumentException.class,
                () -> service.transition(inProgress.id(), new TripCommand.Complete(99.0, null), "driver"));
        assertSame(inProgress, persisted.get());

        var futureStart = trip("IN_PROGRESS", UUID.randomUUID(), UUID.randomUUID(), NOW.plusMinutes(1), 100.0);
        given(futureStart);
        assertThrows(IllegalArgumentException.class,
                () -> service.transition(futureStart.id(), new TripCommand.Complete(101.0, null), "driver"));
        assertSame(futureStart, persisted.get());
        verify(repository, never()).save(any());
        assertTrue(audit.isEmpty());
    }

    @Test
    void cancellationRequiresReasonAndIsRejectedAfterClose() {
        given(trip("APPROVED"));
        assertThrows(IllegalArgumentException.class,
                () -> service.transition(persisted.get().id(), new TripCommand.Cancel(null), "dispatcher"));
        var cancelled = service.transition(persisted.get().id(), new TripCommand.Cancel("Customer request"),
                "dispatcher");
        assertEquals("CANCELLED", cancelled.status());
        assertHistory("APPROVED", "CANCELLED", "TRIP_CANCELLED", "dispatcher");

        var closed = trip("CLOSED");
        given(closed);
        audit.clear();
        var error = assertThrows(ConflictException.class,
                () -> service.transition(closed.id(), new TripCommand.Cancel("Too late"), "dispatcher"));
        assertEquals("TRIP_NOT_CANCELLABLE", error.code());
        assertSame(closed, persisted.get());

        for (var status : List.of("IN_PROGRESS", "COMPLETED")) {
            var notCancellable = trip(status);
            given(notCancellable);
            audit.clear();
            var statusError = assertThrows(ConflictException.class,
                    () -> service.transition(notCancellable.id(), new TripCommand.Cancel("Too late"), "dispatcher"));
            assertEquals("TRIP_NOT_CANCELLABLE", statusError.code());
            assertSame(notCancellable, persisted.get());
            assertTrue(audit.isEmpty());
        }
    }

    @Test
    void ordinaryUpdateCannotChangeEligibilityFieldsAfterAssignment() {
        var assigned = trip("ASSIGNED", UUID.randomUUID(), UUID.randomUUID(), null, null);
        given(assigned);
        var requested = new Trip(assigned.id(), assigned.tripNumber(), assigned.customerId(), assigned.departmentId(),
                assigned.projectId(), assigned.routeId(), assigned.priority(), assigned.status(),
                assigned.originLocationId(), assigned.destinationLocationId(), assigned.requestedStartTime().plusHours(1),
                assigned.requestedEndTime().plusHours(1), assigned.requiredVehicleTypeId(), assigned.requiredCapacityKg(),
                assigned.cargoDescription(), assigned.passengerCount(), assigned.customerInstructions(), assigned.notes(),
                assigned.vehicleId(), assigned.driverId(), assigned.actualStartTime(), assigned.actualEndTime(),
                assigned.startOdometerKm(), assigned.endOdometerKm(), assigned.completionRemarks(), assigned.createdAt(),
                assigned.updatedAt());

        var error = assertThrows(ConflictException.class, () -> service.update(assigned.id(), requested));

        assertEquals("ASSIGNED_TRIP_UPDATE_REQUIRES_REVALIDATION", error.code());
        assertSame(assigned, persisted.get());
        verify(repository, never()).save(any());
    }

    @Test
    void completedAndClosedTripsCannotBeEdited() {
        for (var status : List.of("COMPLETED", "CLOSED")) {
            var terminal = trip(status);
            given(terminal);

            var error = assertThrows(ConflictException.class, () -> service.update(terminal.id(), terminal));

            assertEquals("TRIP_NOT_EDITABLE", error.code());
            assertSame(terminal, persisted.get());
            verify(repository, never()).save(any());
        }
    }

    private void assertHistory(String from, String to, String action, String actor) {
        var entry = audit.getLast();
        assertEquals(from, entry.fromStatus());
        assertEquals(to, entry.toStatus());
        assertEquals(action, entry.action());
        assertEquals(actor, entry.actor());
        assertEquals(NOW, entry.occurredAt());
    }

    private void given(Trip trip) {
        persisted.set(trip);
        clearInvocations(repository, history);
    }

    private Trip trip(String status) {
        return trip(status, null, null, null, null);
    }

    private Trip trip(String status, UUID vehicleId, UUID driverId, OffsetDateTime actualStart,
                      Double startOdometer) {
        return new Trip(UUID.randomUUID(), "TRIP-" + UUID.randomUUID(), null, null, null, null, "NORMAL", status,
                UUID.randomUUID(), UUID.randomUUID(), NOW.plusDays(1), NOW.plusDays(2), UUID.randomUUID(),
                1000.0, "Cargo", 0, null, null, vehicleId, driverId, actualStart, null, startOdometer, null,
                null, NOW.minusDays(1), NOW.minusDays(1));
    }

    private static final class CountingTransaction implements TripTransaction {
        private int executions;

        @Override
        public <T> T execute(Supplier<T> operation) {
            executions++;
            return operation.get();
        }
    }
}
