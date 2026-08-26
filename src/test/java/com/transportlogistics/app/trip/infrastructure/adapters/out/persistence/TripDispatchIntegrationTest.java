package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripDispatchRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.transportlogistics.app.support.ReferenceFixtures.*;

@SpringBootTest
class TripDispatchIntegrationTest {
    @Autowired TripUseCase trips;
    @Autowired TripRepository tripRepository;
    @Autowired TripDispatchRepository dispatches;
    @Autowired VehicleRepository vehicles;
    @Autowired DriverRepository drivers;
    @Autowired DriverLicenseRepository licenses;
    @Autowired JdbcTemplate jdbc;

    @Test
    void persistsDispatchMetadataAfterFreshVehicleAndDriverValidation() {
        var vehicle = vehicle();
        var driver = driver();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        drivers.save(driver);
        licenses.save(license(driver.id()));
        var trip = trip();
        tripLocations(jdbc, trip);
        tripRepository.save(trip);

        trips.assignVehicle(trip.id(), vehicle.id(), "allocator");
        trips.assignDriver(trip.id(), driver.id(), "B", "allocator");
        var dispatched = trips.dispatch(trip.id(), "dispatcher", "Gate 4");

        assertEquals("DISPATCHED", dispatched.status());
        var metadata = dispatches.findByTripId(trip.id()).orElseThrow();
        assertEquals("dispatcher", metadata.dispatchedBy());
        assertEquals("Gate 4", metadata.remarks());
    }

    private Vehicle vehicle() {
        return new Vehicle(UUID.randomUUID(), "REG-" + suffix(), null, null, UUID.randomUUID(), UUID.randomUUID(),
                "Maker", "Model", 2025, "OWNED", "AVAILABLE", 1000.0, null, 5000.0, true);
    }

    private Driver driver() {
        return new Driver(UUID.randomUUID(), "EMP-" + suffix(), "Alex", "Driver", null, null,
                "AVAILABLE", true);
    }

    private DriverLicense license(UUID driverId) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new DriverLicense(UUID.randomUUID(), driverId, "DL-" + suffix(), "B",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), DriverLicenseStatus.ACTIVE, true,
                now, now, "test", "test");
    }

    private Trip trip() {
        var now = OffsetDateTime.parse("2026-05-01T00:00:00Z");
        return new Trip(UUID.randomUUID(), "TRIP-DSP-" + suffix(), null, null, null, null, "NORMAL", "APPROVED",
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.parse("2026-06-01T08:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"), null, null, null, null, null, null, null,
                null, null, null, null, null, null, now, now);
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
