package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripTransaction;
import com.transportlogistics.app.trip.application.ports.out.TripDispatchRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripServiceDriverEligibilityTest {
    @Test
    void assignmentChecksRequiredClassAtTripStartButHistoricalReadsRemainValid() {
        var repository = mock(TripRepository.class);
        var eligibility = mock(DriverEligibilityPort.class);
        var history = mock(TripHistoryRepository.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(repository, eligibility, history);

        var assigned = service.assignDriver(trip.id(), trip.driverId(), "B", "dispatcher");
        verify(eligibility).assertEligible(trip.driverId(), "B", trip.requestedStartTime(),
                trip.requestedEndTime());
        assertEquals(trip.driverId(), assigned.driverId());
        assertEquals("ASSIGNED", assigned.status());
        verify(history).save(argThat(entry -> entry.action().equals("DRIVER_REASSIGNED")
                && entry.driverId().equals(trip.driverId()) && entry.actor().equals("dispatcher")));

        reset(eligibility);
        assertEquals(trip.driverId(), service.get(trip.id()).driverId());
        verifyNoInteractions(eligibility);
    }

    @Test
    void expiredOrWrongClassLicensePreventsAssignmentWithoutChangingTrip() {
        var repository = mock(TripRepository.class);
        var eligibility = mock(DriverEligibilityPort.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        doThrow(new IllegalArgumentException("REQUIRED_LICENSE_CLASS_MISSING_OR_EXPIRED"))
                .when(eligibility).assertEligible(eq(trip.driverId()), eq("C"), any(), any());
        var service = service(repository, eligibility, mock(TripHistoryRepository.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.assignDriver(trip.id(), trip.driverId(), "C", "dispatcher"));
        verify(repository, never()).save(any());
    }

    @Test
    void overlapProducesConflictWithoutPersistingAssignmentOrAudit() {
        var repository = mock(TripRepository.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        when(repository.hasOverlappingDriverAssignment(trip.driverId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.id())).thenReturn(true);
        var history = mock(TripHistoryRepository.class);
        var service = service(repository, mock(DriverEligibilityPort.class), history);

        assertThrows(ConflictException.class,
                () -> service.assignDriver(trip.id(), trip.driverId(), "B", "dispatcher"));

        verify(repository, never()).save(any());
        verifyNoInteractions(history);
    }

    @Test
    void assignmentRequiresValidStateAndLicenseClass() {
        var repository = mock(TripRepository.class);
        var submitted = trip("SUBMITTED");
        when(repository.findById(submitted.id())).thenReturn(Optional.of(submitted));
        var eligibility = mock(DriverEligibilityPort.class);
        var service = service(repository, eligibility, mock(TripHistoryRepository.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.assignDriver(submitted.id(), submitted.driverId(), "B", "dispatcher"));
        assertThrows(IllegalArgumentException.class,
                () -> service.assignDriver(submitted.id(), submitted.driverId(), " ", "dispatcher"));
        verifyNoInteractions(eligibility);
    }

    private Trip trip() {
        return trip("APPROVED");
    }

    private Trip trip(String status) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-1", null, null, null, null, "NORMAL", status,
                UUID.randomUUID(), UUID.randomUUID(), now.plusDays(1), now.plusDays(2), null, null, null, null,
                null, null, null, UUID.randomUUID(), null, null, null, null, null, now, now);
    }

    private TripService service(TripRepository repository, DriverEligibilityPort eligibility,
                                TripHistoryRepository history) {
        return new TripService(repository, mock(VehicleEligibilityPort.class), eligibility,
                history, new TripTransaction() {
                    @Override
                    public <T> T execute(Supplier<T> operation) {
                        return operation.get();
                    }
                }, mock(TripDispatchRepository.class));
    }
}
