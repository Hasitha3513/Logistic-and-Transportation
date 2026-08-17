package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.transportlogistics.app.support.ReferenceFixtures.*;

@SpringBootTest
class TripLifecycleIntegrationTest {
    @Autowired TripUseCase trips;
    @Autowired TripRepository tripRepository;
    @Autowired TripHistoryRepository history;
    @Autowired VehicleRepository vehicles;
    @Autowired DriverRepository drivers;
    @Autowired DriverLicenseRepository licenses;
    @Autowired JdbcTemplate jdbc;

    @Test
    void persistsTheCompleteAuthorizedLifecycleAndAppendOnlyHistory() {
        var vehicle = vehicle();
        var driver = driver();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        drivers.save(driver);
        licenses.save(license(driver.id()));
        appUser(jdbc, "driver");
        var draft = draft();
        tripLocations(jdbc, draft);
        tripRepository.save(draft);

        trips.transition(draft.id(), new TripCommand.Submit(), "requester");
        trips.transition(draft.id(), new TripCommand.Approve(), "approver");
        trips.assignVehicle(draft.id(), vehicle.id(), "allocator");
        trips.assignDriver(draft.id(), driver.id(), "B", "allocator");
        trips.dispatch(draft.id(), "dispatcher", "Gate 4");
        trips.transition(draft.id(), new TripCommand.Start(1000.0), "driver");
        trips.transition(draft.id(), new TripCommand.Complete(1050.0, "Delivered"), "driver");
        var closed = trips.transition(draft.id(), new TripCommand.Close(), "supervisor");

        assertEquals("CLOSED", closed.status());
        var entries = history.findByTripId(draft.id());
        assertEquals(List.of("TRIP_SUBMITTED", "TRIP_APPROVED", "VEHICLE_ASSIGNED", "DRIVER_ASSIGNED",
                        "TRIP_DISPATCHED", "TRIP_STARTED", "TRIP_COMPLETED", "TRIP_CLOSED"),
                entries.stream().map(entry -> entry.action()).toList());
        assertEquals(List.of("requester", "approver", "allocator", "allocator", "dispatcher", "driver",
                        "driver", "supervisor"), entries.stream().map(entry -> entry.actor()).toList());
        assertTrue(entries.stream().allMatch(entry -> entry.occurredAt() != null));
        assertEquals("DRAFT", entries.getFirst().fromStatus());
        assertEquals("CLOSED", entries.getLast().toStatus());

        var readingSources = jdbc.queryForList(
                "SELECT source_type FROM vehicle_reading WHERE source_reference_id = ? ORDER BY recorded_at ASC",
                String.class, draft.id());
        assertEquals(List.of("TRIP_START", "TRIP_END"), readingSources);

        var updatedVehicle = vehicles.findById(vehicle.id()).orElseThrow();
        assertEquals(1050.0, updatedVehicle.currentOdometerKm());
    }

    private Vehicle vehicle() {
        return new Vehicle(UUID.randomUUID(), "REG-LIFE-" + suffix(), null, null, UUID.randomUUID(),
                UUID.randomUUID(), "Maker", "Model", 2025, "OWNED", "AVAILABLE", 900.0, null, 5000.0, true);
    }

    private Driver driver() {
        return new Driver(UUID.randomUUID(), "EMP-LIFE-" + suffix(), "Alex", "Driver", null, null,
                "AVAILABLE", true);
    }

    private DriverLicense license(UUID driverId) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new DriverLicense(UUID.randomUUID(), driverId, "DL-LIFE-" + suffix(), "B",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 12, 31), DriverLicenseStatus.ACTIVE, true,
                now, now, "test", "test");
    }

    private Trip draft() {
        var now = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-LIFE-" + suffix(), null, null, null, null, "NORMAL", "DRAFT",
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.parse("2026-09-01T08:00:00Z"),
                OffsetDateTime.parse("2026-09-01T12:00:00Z"), null, null, "Cargo", 0, null, null,
                null, null, null, null, null, null, null, now, now);
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
