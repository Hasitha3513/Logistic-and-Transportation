package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static com.transportlogistics.app.support.ReferenceFixtures.*;

@SpringBootTest
class ConcurrentVehicleAssignmentIntegrationTest {
    @Autowired TripUseCase trips;
    @Autowired TripRepository tripRepository;
    @Autowired VehicleRepository vehicles;
    @Autowired TripHistoryRepository history;
    @Autowired JdbcTemplate jdbc;

    @Test
    void concurrentOverlappingAssignmentsAllowExactlyOneAllocation() throws Exception {
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        var start = OffsetDateTime.parse("2026-06-01T08:00:00Z");
        var first = trip("TRIP-CON-A-" + suffix(), start, start.plusHours(3));
        var second = trip("TRIP-CON-B-" + suffix(), start.plusHours(1), start.plusHours(4));
        tripLocations(jdbc, first);
        tripLocations(jdbc, second);
        tripRepository.save(first);
        tripRepository.save(second);

        var ready = new CountDownLatch(2);
        var startTogether = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> assign(first.id(), vehicle.id(), ready, startTogether));
            var secondResult = executor.submit(() -> assign(second.id(), vehicle.id(), ready, startTogether));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            startTogether.countDown();

            var results = List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(Trip.class::isInstance).count());
            assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count());
        }

        var persisted = List.of(tripRepository.findById(first.id()).orElseThrow(),
                tripRepository.findById(second.id()).orElseThrow());
        assertEquals(1, persisted.stream().filter(t -> vehicle.id().equals(t.vehicleId())).count());
        assertEquals(0, persisted.stream().filter(t -> "ASSIGNED".equals(t.status())).count());
        assertEquals(2, persisted.stream().filter(t -> "APPROVED".equals(t.status())).count());
        assertEquals(1, history.findByTripId(first.id()).size() + history.findByTripId(second.id()).size());
    }

    private Object assign(UUID tripId, UUID vehicleId, CountDownLatch ready, CountDownLatch startTogether) {
        ready.countDown();
        try {
            if (!startTogether.await(10, TimeUnit.SECONDS)) {
                return new IllegalStateException("Concurrent assignment did not start in time");
            }
            return trips.assignVehicle(tripId, vehicleId, "concurrency-test");
        } catch (RuntimeException exception) {
            return exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return exception;
        }
    }

    private Vehicle vehicle() {
        return new Vehicle(UUID.randomUUID(), "REG-" + suffix(), null, null, UUID.randomUUID(), UUID.randomUUID(),
                "Maker", "Model", 2025, "OWNED", "AVAILABLE", 1000.0, null, 5000.0, true);
    }

    private Trip trip(String tripNumber, OffsetDateTime start, OffsetDateTime end) {
        var now = OffsetDateTime.parse("2026-05-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), tripNumber, null, null, null, null, "NORMAL", "APPROVED",
                UUID.randomUUID(), UUID.randomUUID(), start, end, null, null, null, null, null, null, null,
                null, null, null, null, null, null, now, now);
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
