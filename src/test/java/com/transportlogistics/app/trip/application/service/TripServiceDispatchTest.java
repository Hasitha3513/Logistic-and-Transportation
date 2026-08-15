package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.trip.application.ports.out.*;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripServiceDispatchTest {
    private TripRepository trips;
    private VehicleEligibilityPort vehicles;
    private DriverEligibilityPort drivers;
    private TripHistoryRepository history;
    private TripDispatchRepository dispatches;
    private TripService service;
    private Trip trip;

    @BeforeEach
    void setUp() {
        trips = mock(TripRepository.class);
        vehicles = mock(VehicleEligibilityPort.class);
        drivers = mock(DriverEligibilityPort.class);
        history = mock(TripHistoryRepository.class);
        dispatches = mock(TripDispatchRepository.class);
        service = new TripService(trips, vehicles, drivers, history, new DirectTransaction(), dispatches);
        trip = trip("ASSIGNED", UUID.randomUUID(), UUID.randomUUID());
        when(trips.findById(trip.id())).thenReturn(Optional.of(trip));
        when(trips.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(history.findCurrentDriverLicenseClass(trip.id(), trip.driverId())).thenReturn(Optional.of("B"));
    }

    @Test
    void dispatchRevalidatesEverythingAndRecordsMetadataAndHistory() {
        var result = service.dispatch(trip.id(), "dispatcher", "Gate 4");

        assertEquals("DISPATCHED", result.status());
        verify(vehicles).assertEligibleForAssignment(trip.vehicleId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.requiredVehicleTypeId(), trip.requiredCapacityKg());
        verify(trips).hasOverlappingVehicleAllocation(trip.vehicleId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.id());
        verify(drivers).assertEligible(trip.driverId(), "B", trip.requestedStartTime(), trip.requestedEndTime());
        verify(trips).hasOverlappingDriverAssignment(trip.driverId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.id());
        verify(dispatches).save(argThat(record -> record.dispatchedBy().equals("dispatcher")
                && record.remarks().equals("Gate 4")));
        verify(history).save(argThat(entry -> entry.action().equals("TRIP_DISPATCHED")
                && entry.licenseClass().equals("B") && entry.actor().equals("dispatcher")));
    }

    @Test
    void rejectsWrongTripStatus() {
        givenTrip(trip("APPROVED", trip.vehicleId(), trip.driverId()));
        assertRejected(ConflictException.class);
        verifyNoInteractions(vehicles, drivers, dispatches);
    }

    @Test
    void rejectsMissingVehicleAssignment() {
        givenTrip(trip("ASSIGNED", null, trip.driverId()));
        assertRejected(ConflictException.class);
    }

    @Test
    void rejectsMissingDriverAssignment() {
        givenTrip(trip("ASSIGNED", trip.vehicleId(), null));
        assertRejected(ConflictException.class);
    }

    @Test
    void rejectsMissingAssignedLicenseClass() {
        when(history.findCurrentDriverLicenseClass(trip.id(), trip.driverId())).thenReturn(Optional.empty());
        assertRejected(ConflictException.class);
    }

    @Test
    void rejectsInactiveVehicle() {
        rejectVehicle("INACTIVE");
    }

    @Test
    void rejectsOperationallyIneligibleVehicle() {
        rejectVehicle("OPERATIONALLY_UNAVAILABLE");
    }

    @Test
    void rejectsVehicleWithInvalidMandatoryDocuments() {
        rejectVehicle("MANDATORY_DOCUMENT_EXPIRED");
    }

    @Test
    void rejectsConflictingVehicleAllocation() {
        when(trips.hasOverlappingVehicleAllocation(trip.vehicleId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.id())).thenReturn(true);
        assertRejected(ConflictException.class);
        verifyNoInteractions(drivers);
    }

    @Test
    void rejectsInactiveDriver() {
        rejectDriver("INACTIVE");
    }

    @Test
    void rejectsUnavailableDriver() {
        rejectDriver("OPERATIONALLY_UNAVAILABLE");
    }

    @Test
    void rejectsInvalidDriverLicense() {
        rejectDriver("LICENSE_EXPIRED");
    }

    @Test
    void rejectsWrongDriverLicenseClass() {
        rejectDriver("REQUIRED_LICENSE_CLASS_MISSING");
    }

    @Test
    void rejectsConflictingDriverAssignment() {
        when(trips.hasOverlappingDriverAssignment(trip.driverId(), trip.requestedStartTime(),
                trip.requestedEndTime(), trip.id())).thenReturn(true);
        assertRejected(ConflictException.class);
    }

    private void rejectVehicle(String reason) {
        doThrow(new IllegalArgumentException(reason)).when(vehicles).assertEligibleForAssignment(
                trip.vehicleId(), trip.requestedStartTime(), trip.requestedEndTime(),
                trip.requiredVehicleTypeId(), trip.requiredCapacityKg());
        assertRejected(IllegalArgumentException.class);
        verifyNoInteractions(drivers);
    }

    private void rejectDriver(String reason) {
        doThrow(new IllegalArgumentException(reason)).when(drivers).assertEligible(
                trip.driverId(), "B", trip.requestedStartTime(), trip.requestedEndTime());
        assertRejected(IllegalArgumentException.class);
    }

    private void assertRejected(Class<? extends RuntimeException> type) {
        assertThrows(type, () -> service.dispatch(trip.id(), "dispatcher", null));
        verify(trips, never()).save(any());
        verify(dispatches, never()).save(any());
        verify(history, never()).save(any());
    }

    private void givenTrip(Trip value) {
        trip = value;
        when(trips.findById(value.id())).thenReturn(Optional.of(value));
    }

    private Trip trip(String status, UUID vehicleId, UUID driverId) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-1", null, null, null, null, "NORMAL", status,
                UUID.randomUUID(), UUID.randomUUID(), now.plusDays(1), now.plusDays(2), UUID.randomUUID(),
                1000.0, null, null, null, null, vehicleId, driverId, null, null, null, null, null, now, now);
    }

    private static final class DirectTransaction implements TripTransaction {
        @Override
        public <T> T execute(Supplier<T> operation) {
            return operation.get();
        }
    }
}
