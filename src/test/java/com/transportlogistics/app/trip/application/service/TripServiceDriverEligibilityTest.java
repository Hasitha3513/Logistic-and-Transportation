package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripServiceDriverEligibilityTest {
    @Test
    void assignmentChecksRequiredClassAtTripStartButHistoricalReadsRemainValid() {
        var repository = mock(TripRepository.class);
        var eligibility = mock(DriverEligibilityPort.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new TripService(repository, mock(VehicleEligibilityPort.class), eligibility);

        var assigned = service.assignDriver(trip.id(), trip.driverId(), "B");
        verify(eligibility).assertEligible(trip.driverId(), "B", trip.requestedStartTime().toLocalDate());
        assertEquals(trip.driverId(), assigned.driverId());

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
                .when(eligibility).assertEligible(eq(trip.driverId()), eq("C"), any());
        var service = new TripService(repository, mock(VehicleEligibilityPort.class), eligibility);

        assertThrows(IllegalArgumentException.class,
                () -> service.assignDriver(trip.id(), trip.driverId(), "C"));
        verify(repository, never()).save(any());
    }

    private Trip trip() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-1", null, null, null, null, "NORMAL", "APPROVED",
                UUID.randomUUID(), UUID.randomUUID(), now.plusDays(1), now.plusDays(2), null, null, null, null,
                null, null, null, UUID.randomUUID(), null, null, null, null, null, now, now);
    }
}
