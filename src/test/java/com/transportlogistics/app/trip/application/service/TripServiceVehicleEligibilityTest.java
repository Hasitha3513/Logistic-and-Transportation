package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripServiceVehicleEligibilityTest {
    @Test
    void vehicleEligibilityIsCheckedOnAssignmentAndDispatch() {
        var repository = mock(TripRepository.class);
        var eligibility = mock(VehicleEligibilityPort.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new TripService(repository, eligibility, mock(DriverEligibilityPort.class));

        service.assignVehicle(trip.id(), trip.vehicleId());
        service.transition(trip.id(), new TripCommand.Dispatch());

        verify(eligibility, times(2)).assertEligible(eq(trip.vehicleId()), any());
    }

    @Test
    void ineligibleVehicleCannotBeAssigned() {
        var repository = mock(TripRepository.class);
        var eligibility = mock(VehicleEligibilityPort.class);
        var trip = trip();
        when(repository.findById(trip.id())).thenReturn(Optional.of(trip));
        doThrow(new IllegalArgumentException("MANDATORY_DOCUMENT_EXPIRED"))
                .when(eligibility).assertEligible(eq(trip.vehicleId()), any());
        var service = new TripService(repository, eligibility, mock(DriverEligibilityPort.class));

        assertThrows(IllegalArgumentException.class, () -> service.assignVehicle(trip.id(), trip.vehicleId()));
        verify(repository, never()).save(any());
    }

    private Trip trip() {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-1", null, null, null, null, "NORMAL", "APPROVED",
                UUID.randomUUID(), UUID.randomUUID(), now.plusDays(1), now.plusDays(2), null, null, null, null,
                null, null, UUID.randomUUID(), null, null, null, null, null, null, now, now);
    }
}
