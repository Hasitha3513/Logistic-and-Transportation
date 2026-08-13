package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripTransaction;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripServiceVehicleEligibilityTest {
    @Test
    void vehicleEligibilityIsCheckedOnAssignmentAndDispatch() {
        var repository = mock(TripRepository.class);
        var eligibility = mock(VehicleEligibilityPort.class);
        var history = mock(TripHistoryRepository.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(repository, eligibility, history);

        var assigned = service.assignVehicle(trip.id(), trip.vehicleId(), "dispatcher");
        service.transition(trip.id(), new TripCommand.Dispatch());

        assertEquals("ASSIGNED", assigned.status());
        verify(eligibility).assertEligibleForAssignment(trip.vehicleId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.requiredVehicleTypeId(), trip.requiredCapacityKg());
        verify(eligibility).assertEligibleForDispatch(trip.vehicleId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.requiredVehicleTypeId(), trip.requiredCapacityKg(), trip.id());
        verify(history).save(argThat(entry -> entry.action().equals("VEHICLE_REASSIGNED")
                && entry.actor().equals("dispatcher") && entry.toStatus().equals("ASSIGNED")));
    }

    @Test
    void ineligibleVehicleCannotBeAssigned() {
        var repository = mock(TripRepository.class);
        var eligibility = mock(VehicleEligibilityPort.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        doThrow(new IllegalArgumentException("MANDATORY_DOCUMENT_EXPIRED"))
                .when(eligibility).assertEligibleForAssignment(eq(trip.vehicleId()), any(), any(), any(), any());
        var service = service(repository, eligibility, mock(TripHistoryRepository.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.assignVehicle(trip.id(), trip.vehicleId(), "dispatcher"));
        verify(repository, never()).save(any());
    }

    @Test
    void allocationConflictReturnsConflictWithoutChangingTripOrHistory() {
        var repository = mock(TripRepository.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        when(repository.hasOverlappingVehicleAllocation(trip.vehicleId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.id())).thenReturn(true);
        var history = mock(TripHistoryRepository.class);
        var service = service(repository, mock(VehicleEligibilityPort.class), history);

        assertThrows(ConflictException.class,
                () -> service.assignVehicle(trip.id(), trip.vehicleId(), "dispatcher"));

        verify(repository, never()).save(any());
        verifyNoInteractions(history);
    }

    @Test
    void assignmentRequiresApprovedOrAssignedTrip() {
        var repository = mock(TripRepository.class);
        var trip = trip("SUBMITTED");
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        var eligibility = mock(VehicleEligibilityPort.class);
        var service = service(repository, eligibility, mock(TripHistoryRepository.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.assignVehicle(trip.id(), trip.vehicleId(), "dispatcher"));
        verifyNoInteractions(eligibility);
    }

    private TripService service(TripRepository repository, VehicleEligibilityPort eligibility,
                                TripHistoryRepository history) {
        return new TripService(repository, eligibility, mock(DriverEligibilityPort.class), history,
                new DirectTransaction());
    }

    private Trip trip() {
        return trip("APPROVED");
    }

    private Trip trip(String status) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-1", null, null, null, null, "NORMAL", status,
                UUID.randomUUID(), UUID.randomUUID(), now.plusDays(1), now.plusDays(2), null, null, null, null,
                null, null, UUID.randomUUID(), null, null, null, null, null, null, now, now);
    }

    private static final class DirectTransaction implements TripTransaction {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
