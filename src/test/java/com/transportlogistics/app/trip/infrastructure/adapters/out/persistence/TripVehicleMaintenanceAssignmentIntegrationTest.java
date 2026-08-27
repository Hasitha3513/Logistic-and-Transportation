package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.in.MaintenanceScheduleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.MaintenanceScheduleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.MaintenanceSchedule;
import com.transportlogistics.app.fleet.domain.model.MaintenanceStatus;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TripVehicleMaintenanceAssignmentIntegrationTest {

    @Autowired private TripUseCase trips;
    @Autowired private TripRepository tripRepository;
    @Autowired private VehicleRepository vehicles;
    @Autowired private MaintenanceScheduleRepository maintenanceSchedules;
    @Autowired private MaintenanceScheduleUseCase maintenanceScheduleUseCase;
    @Autowired private JdbcTemplate jdbc;

    private final OffsetDateTime tripStart = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private final OffsetDateTime tripEnd = OffsetDateTime.parse("2026-06-01T14:00:00Z");

    @Test
    void A_rejectsTripAssignmentWhenScheduledMaintenanceOverlaps() {
        var vehicle = createVehicle();
        var trip = createTrip(tripStart, tripEnd);

        // Maintenance schedule: 09:00 to 12:00
        var maint = new MaintenanceSchedule(
                UUID.randomUUID(), vehicle.id(), "Routine Service",
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                MaintenanceStatus.SCHEDULED, "Scheduled PM", "Shop A", new BigDecimal("150.00"),
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        maintenanceSchedules.save(maint);

        assertThatThrownBy(() -> trips.assignVehicle(trip.id(), vehicle.id(), "dispatcher"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MAINTENANCE_BLOCKED");
    }

    @Test
    void B_rejectsMaintenanceCreationWhenActiveTripAllocationOverlaps() {
        var vehicle = createVehicle();
        var trip = createTrip(tripStart, tripEnd);

        // Assign vehicle to trip (trip is now 10:00 to 14:00)
        trips.assignVehicle(trip.id(), vehicle.id(), "dispatcher");

        // Attempt to create maintenance from 11:00 to 13:00 (overlaps)
        var createCmd = new MaintenanceScheduleUseCase.CreateCommand(
                "PM Service",
                OffsetDateTime.parse("2026-06-01T11:00:00Z"),
                OffsetDateTime.parse("2026-06-01T13:00:00Z"),
                "PM", null, null
        );

        assertThatThrownBy(() -> maintenanceScheduleUseCase.create(vehicle.id(), createCmd, "dispatcher"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active trip allocation");
    }

    @Test
    void C_rejectsMaintenanceReschedulingWhenActiveTripAllocationOverlaps() {
        var vehicle = createVehicle();
        var trip = createTrip(tripStart, tripEnd);

        // Assign vehicle to trip (trip is 10:00 to 14:00)
        trips.assignVehicle(trip.id(), vehicle.id(), "dispatcher");

        // Create a non-overlapping maintenance schedule: 15:00 to 18:00
        var maint = maintenanceScheduleUseCase.create(vehicle.id(), new MaintenanceScheduleUseCase.CreateCommand(
                "PM Service",
                OffsetDateTime.parse("2026-06-01T15:00:00Z"),
                OffsetDateTime.parse("2026-06-01T18:00:00Z"),
                "PM", null, null
        ), "dispatcher");

        // Attempt to reschedule into trip's window: 11:00 to 13:00
        var updateCmd = new MaintenanceScheduleUseCase.UpdateCommand(
                null,
                OffsetDateTime.parse("2026-06-01T11:00:00Z"),
                OffsetDateTime.parse("2026-06-01T13:00:00Z"),
                null, null, null, null
        );

        assertThatThrownBy(() -> maintenanceScheduleUseCase.update(vehicle.id(), maint.id(), updateCmd, "dispatcher"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active trip allocation");
    }

    @Test
    void D_allowsBoundaryTripEndingExactlyWhenMaintenanceStarts() {
        var vehicle = createVehicle();
        // Trip is 08:00 to 10:00
        var trip = createTrip(OffsetDateTime.parse("2026-06-01T08:00:00Z"), OffsetDateTime.parse("2026-06-01T10:00:00Z"));

        // Maintenance is 10:00 to 12:00
        var maint = new MaintenanceSchedule(
                UUID.randomUUID(), vehicle.id(), "PM Service",
                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                MaintenanceStatus.SCHEDULED, "Morning PM", "Shop A", new BigDecimal("150.00"),
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        maintenanceSchedules.save(maint);

        // Trip assignment should succeed
        var assigned = trips.assignVehicle(trip.id(), vehicle.id(), "dispatcher");
        assertThat(assigned).isNotNull();
        assertThat(assigned.vehicleId()).isEqualTo(vehicle.id());
    }

    @Test
    void E_allowsOppositeBoundaryTripStartingExactlyWhenMaintenanceEnds() {
        var vehicle = createVehicle();
        // Maintenance is 10:00 to 12:00
        var maint = new MaintenanceSchedule(
                UUID.randomUUID(), vehicle.id(), "PM Service",
                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                MaintenanceStatus.SCHEDULED, "Morning PM", "Shop A", new BigDecimal("150.00"),
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        maintenanceSchedules.save(maint);

        // Trip is 12:00 to 14:00
        var trip = createTrip(OffsetDateTime.parse("2026-06-01T12:00:00Z"), OffsetDateTime.parse("2026-06-01T14:00:00Z"));

        // Trip assignment should succeed
        var assigned = trips.assignVehicle(trip.id(), vehicle.id(), "dispatcher");
        assertThat(assigned).isNotNull();
        assertThat(assigned.vehicleId()).isEqualTo(vehicle.id());
    }

    @Test
    void F_allowsTripAssignmentWhenScheduledMaintenanceIsCancelled() {
        var vehicle = createVehicle();
        var trip = createTrip(tripStart, tripEnd);

        // Create a cancelled maintenance schedule
        var maint = new MaintenanceSchedule(
                UUID.randomUUID(), vehicle.id(), "Routine Service",
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                MaintenanceStatus.CANCELLED, "Cancelled PM", "Shop A", new BigDecimal("150.00"),
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        maintenanceSchedules.save(maint);

        var assigned = trips.assignVehicle(trip.id(), vehicle.id(), "dispatcher");
        assertThat(assigned).isNotNull();
        assertThat(assigned.vehicleId()).isEqualTo(vehicle.id());
    }

    @Test
    void G_allowsTripAssignmentWhenScheduledMaintenanceIsCompleted() {
        var vehicle = createVehicle();
        var trip = createTrip(tripStart, tripEnd);

        // Create a completed maintenance schedule
        var maint = new MaintenanceSchedule(
                UUID.randomUUID(), vehicle.id(), "Routine Service",
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                MaintenanceStatus.COMPLETED, "Completed PM", "Shop A", new BigDecimal("150.00"),
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        maintenanceSchedules.save(maint);

        var assigned = trips.assignVehicle(trip.id(), vehicle.id(), "dispatcher");
        assertThat(assigned).isNotNull();
        assertThat(assigned.vehicleId()).isEqualTo(vehicle.id());
    }

    private Vehicle createVehicle() {
        var vehicle = new Vehicle(UUID.randomUUID(), "REG-" + suffix(), null, null, UUID.randomUUID(), UUID.randomUUID(),
                "Maker", "Model", 2025, "OWNED", "AVAILABLE", 1000.0, null, 5000.0, true);
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        return vehicle;
    }

    private Trip createTrip(OffsetDateTime start, OffsetDateTime end) {
        var trip = new Trip(UUID.randomUUID(), "TRIP-MAINT-" + suffix(), null, null, null, null, "NORMAL", "APPROVED",
                UUID.randomUUID(), UUID.randomUUID(), start, end, null, null, null, null, null, null, null,
                null, null, null, null, null, null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        tripLocations(jdbc, trip);
        tripRepository.save(trip);
        return trip;
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
