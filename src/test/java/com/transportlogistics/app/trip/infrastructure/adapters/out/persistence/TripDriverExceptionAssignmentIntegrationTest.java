package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.in.DriverExceptionUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverExceptionRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.tripLocations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TripDriverExceptionAssignmentIntegrationTest {

    @Autowired private TripUseCase trips;
    @Autowired private TripRepository tripRepository;
    @Autowired private DriverRepository drivers;
    @Autowired private DriverLicenseRepository licenses;
    @Autowired private DriverExceptionRepository driverExceptions;
    @Autowired private DriverExceptionUseCase driverExceptionUseCase;
    @Autowired private JdbcTemplate jdbc;

    private final OffsetDateTime tripStart = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private final OffsetDateTime tripEnd = OffsetDateTime.parse("2026-06-01T14:00:00Z");

    @Test
    void A_rejectsTripDriverAssignmentWhenScheduledExceptionOverlaps() {
        var driver = createDriverWithLicense();
        var trip = createTrip(tripStart, tripEnd);

        // Driver has leave from 09:00 to 12:00
        var exception = new DriverException(
                UUID.randomUUID(), driver.id(), DriverExceptionType.LEAVE,
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                DriverExceptionStatus.SCHEDULED, "Medical", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        driverExceptions.save(exception);

        assertThatThrownBy(() -> trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DRIVER_EXCEPTION_BLOCKED");
    }

    @Test
    void B_rejectsExceptionCreationWhenActiveTripAssignmentOverlaps() {
        var driver = createDriverWithLicense();
        var trip = createTrip(tripStart, tripEnd);

        // Assign driver to trip (trip is 10:00 to 14:00)
        trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher");

        // Attempt to create exception from 11:00 to 13:00 (overlaps)
        var createCmd = new DriverExceptionUseCase.CreateCommand(
                DriverExceptionType.LEAVE,
                OffsetDateTime.parse("2026-06-01T11:00:00Z"),
                OffsetDateTime.parse("2026-06-01T13:00:00Z"),
                "Leave", null
        );

        assertThatThrownBy(() -> driverExceptionUseCase.create(driver.id(), createCmd, "dispatcher"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active trip assignment");
    }

    @Test
    void C_rejectsExceptionReschedulingWhenActiveTripAssignmentOverlaps() {
        var driver = createDriverWithLicense();
        var trip = createTrip(tripStart, tripEnd);

        // Assign driver to trip (trip is 10:00 to 14:00)
        trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher");

        // Create a non-overlapping exception: 15:00 to 18:00
        var exception = driverExceptionUseCase.create(driver.id(), new DriverExceptionUseCase.CreateCommand(
                DriverExceptionType.LEAVE,
                OffsetDateTime.parse("2026-06-01T15:00:00Z"),
                OffsetDateTime.parse("2026-06-01T18:00:00Z"),
                "Leave", null
        ), "dispatcher");

        // Attempt to reschedule into trip's window: 11:00 to 13:00
        var updateCmd = new DriverExceptionUseCase.UpdateCommand(
                null,
                OffsetDateTime.parse("2026-06-01T11:00:00Z"),
                OffsetDateTime.parse("2026-06-01T13:00:00Z"),
                null, null, null
        );

        assertThatThrownBy(() -> driverExceptionUseCase.update(driver.id(), exception.id(), updateCmd, "dispatcher"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active trip assignment");
    }

    @Test
    void D_allowsBoundaryTripEndingExactlyWhenExceptionStarts() {
        var driver = createDriverWithLicense();
        // Trip is 08:00 to 10:00
        var trip = createTrip(OffsetDateTime.parse("2026-06-01T08:00:00Z"), OffsetDateTime.parse("2026-06-01T10:00:00Z"));

        // Exception is 10:00 to 12:00
        var exception = new DriverException(
                UUID.randomUUID(), driver.id(), DriverExceptionType.LEAVE,
                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                DriverExceptionStatus.SCHEDULED, "Afternoon leave", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        driverExceptions.save(exception);

        // Driver assignment should succeed
        var assigned = trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher");
        assertThat(assigned).isNotNull();
        assertThat(assigned.driverId()).isEqualTo(driver.id());
    }

    @Test
    void E_allowsOppositeBoundaryTripStartingExactlyWhenExceptionEnds() {
        var driver = createDriverWithLicense();
        // Exception is 10:00 to 12:00
        var exception = new DriverException(
                UUID.randomUUID(), driver.id(), DriverExceptionType.LEAVE,
                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                DriverExceptionStatus.SCHEDULED, "Morning leave", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        driverExceptions.save(exception);

        // Trip is 12:00 to 14:00
        var trip = createTrip(OffsetDateTime.parse("2026-06-01T12:00:00Z"), OffsetDateTime.parse("2026-06-01T14:00:00Z"));

        // Driver assignment should succeed
        var assigned = trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher");
        assertThat(assigned).isNotNull();
        assertThat(assigned.driverId()).isEqualTo(driver.id());
    }

    @Test
    void F_allowsTripAssignmentWhenExceptionIsCancelled() {
        var driver = createDriverWithLicense();
        var trip = createTrip(tripStart, tripEnd);

        // Create a cancelled exception
        var exception = new DriverException(
                UUID.randomUUID(), driver.id(), DriverExceptionType.LEAVE,
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                DriverExceptionStatus.CANCELLED, "Cancelled", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        driverExceptions.save(exception);

        var assigned = trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher");
        assertThat(assigned).isNotNull();
        assertThat(assigned.driverId()).isEqualTo(driver.id());
    }

    @Test
    void G_allowsTripAssignmentWhenExceptionIsCompleted() {
        var driver = createDriverWithLicense();
        var trip = createTrip(tripStart, tripEnd);

        // Create a completed exception
        var exception = new DriverException(
                UUID.randomUUID(), driver.id(), DriverExceptionType.LEAVE,
                OffsetDateTime.parse("2026-06-01T09:00:00Z"),
                OffsetDateTime.parse("2026-06-01T12:00:00Z"),
                DriverExceptionStatus.COMPLETED, "Completed", null,
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        driverExceptions.save(exception);

        var assigned = trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher");
        assertThat(assigned).isNotNull();
        assertThat(assigned.driverId()).isEqualTo(driver.id());
    }

    private Driver createDriverWithLicense() {
        var driver = new Driver(UUID.randomUUID(), "EMP-EXP-" + suffix(), "Robert", "Smith", "+1234567890",
                "robert." + suffix() + "@example.test", "AVAILABLE", true);
        drivers.save(driver);

        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var license = new DriverLicense(UUID.randomUUID(), driver.id(), "DL-" + suffix(), "B",
                LocalDate.of(2025, 1, 1), LocalDate.of(2028, 1, 1), DriverLicenseStatus.ACTIVE, true,
                now, now, "test", "test");
        licenses.save(license);
        return driver;
    }

    private Trip createTrip(OffsetDateTime start, OffsetDateTime end) {
        var trip = new Trip(UUID.randomUUID(), "TRIP-DRV-EXP-" + suffix(), null, null, null, null, "NORMAL", "APPROVED",
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
