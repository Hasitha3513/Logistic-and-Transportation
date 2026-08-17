package com.transportlogistics.app.trip.application.service;

import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.RouteEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripActorPort;
import com.transportlogistics.app.trip.application.ports.out.TripDispatchRepository;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.TripTransaction;
import com.transportlogistics.app.trip.application.ports.out.TripVehicleReadingPort;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import com.transportlogistics.app.trip.domain.model.TripDispatchRecord;
import com.transportlogistics.app.trip.domain.model.TripHistoryEntry;
import com.transportlogistics.app.trip.domain.model.TripLifecyclePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripVehicleReadingIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-16T08:00:00Z");

    private FakeTripRepository trips;
    private TripVehicleReadingPort readings;
    private TripActorPort actors;
    private VehicleEligibilityPort vehicleEligibility;
    private DriverEligibilityPort driverEligibility;
    private RouteEligibilityPort routeEligibility;
    private TripService service;

    private UUID tripId;
    private UUID vehicleId;
    private UUID driverId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        trips = new FakeTripRepository();
        readings = mock(TripVehicleReadingPort.class);
        actors = mock(TripActorPort.class);
        vehicleEligibility = mock(VehicleEligibilityPort.class);
        driverEligibility = mock(DriverEligibilityPort.class);
        routeEligibility = mock(RouteEligibilityPort.class);

        tripId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        when(actors.find("driver.1")).thenReturn(Optional.of(new TripActorPort.Actor(actorId, "driver.1")));
        when(actors.resolveActorId("driver.1")).thenReturn(actorId);

        var dispatchedTrip = new Trip(tripId, "TRIP-001", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "NORMAL", TripLifecyclePolicy.DISPATCHED, UUID.randomUUID(), UUID.randomUUID(),
                NOW.minusHours(2), NOW.plusHours(4), UUID.randomUUID(), 1000.0, "Cargo", 1, null, null,
                vehicleId, driverId, null, null, null, null, null, NOW.minusDays(1), NOW.minusHours(1));
        trips.save(dispatchedTrip);

        service = new TripService(trips, vehicleEligibility, driverEligibility, routeEligibility,
                new FakeHistoryRepository(), new DirectTransaction(), new FakeDispatchRepository(), readings, actors,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
    }

    @Test
    void tripStartRecordsAuthoritativeReadingWithTripIdAsReference() {
        var result = service.transition(tripId, new TripCommand.Start(10500.0), "driver.1");

        assertEquals(TripLifecyclePolicy.IN_PROGRESS, result.status());
        assertEquals(10500.0, result.startOdometerKm());

        verify(readings).recordTripStart(eq(vehicleId), eq(tripId), eq(10500.0), eq(NOW), eq(actorId));
    }

    @Test
    void tripCompleteRecordsAuthoritativeReadingWithTripIdAsReference() {
        service.transition(tripId, new TripCommand.Start(10500.0), "driver.1");

        var completeTime = NOW.plusHours(2);
        var serviceAtComplete = new TripService(trips, vehicleEligibility, driverEligibility, routeEligibility,
                new FakeHistoryRepository(), new DirectTransaction(), new FakeDispatchRepository(), readings, actors,
                Clock.fixed(completeTime.toInstant(), ZoneOffset.UTC));

        var completed = serviceAtComplete.transition(tripId, new TripCommand.Complete(10750.0, "Trip completed safely"), "driver.1");

        assertEquals(TripLifecyclePolicy.COMPLETED, completed.status());
        assertEquals(10750.0, completed.endOdometerKm());

        verify(readings).recordTripEnd(eq(vehicleId), eq(tripId), eq(10750.0), eq(completeTime), eq(actorId));
    }

    @Test
    void readingFailurePreventsTripStateMutation() {
        doThrow(new ConflictException("VEHICLE_READING_DECREASE", "Decreasing odometer reading rejected"))
                .when(readings).recordTripStart(any(), any(), any(), any(), any());

        assertThrows(ConflictException.class, () -> service.transition(tripId, new TripCommand.Start(9900.0), "driver.1"));

        var tripInRepo = trips.findById(tripId).orElseThrow();
        assertEquals(TripLifecyclePolicy.DISPATCHED, tripInRepo.status());
    }

    private static class DirectTransaction implements TripTransaction {
        @Override public <T> T execute(Supplier<T> operation) { return operation.get(); }
    }

    private static class FakeTripRepository implements TripRepository {
        private final Map<UUID, Trip> values = new HashMap<>();
        @Override public Trip save(Trip trip) { values.put(trip.id(), trip); return trip; }
        @Override public Optional<Trip> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        @Override public Optional<Trip> findByIdForUpdate(UUID id) { return findById(id); }
        @Override public List<Trip> findAll() { return List.copyOf(values.values()); }
        @Override public boolean hasOverlappingVehicleAllocation(UUID vehicleId, OffsetDateTime start, OffsetDateTime end, UUID excludeId) { return false; }
        @Override public boolean hasOverlappingDriverAssignment(UUID driverId, OffsetDateTime start, OffsetDateTime end, UUID excludeId) { return false; }
    }

    private static class FakeHistoryRepository implements TripHistoryRepository {
        private final List<TripHistoryEntry> entries = new ArrayList<>();
        @Override public TripHistoryEntry save(TripHistoryEntry entry) { entries.add(entry); return entry; }
        @Override public List<TripHistoryEntry> findByTripId(UUID tripId) { return entries; }
        @Override public Optional<String> findCurrentDriverLicenseClass(UUID tripId, UUID driverId) { return Optional.of("CLASS-C"); }
    }

    private static class FakeDispatchRepository implements TripDispatchRepository {
        @Override public TripDispatchRecord save(TripDispatchRecord record) { return record; }
        @Override public Optional<TripDispatchRecord> findByTripId(UUID tripId) { return Optional.empty(); }
    }
}