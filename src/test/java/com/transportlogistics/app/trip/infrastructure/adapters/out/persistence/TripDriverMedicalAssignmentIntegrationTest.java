package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.DriverDrugTestRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverMedicalRecordRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.transportlogistics.app.support.ReferenceFixtures.tripLocations;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TripDriverMedicalAssignmentIntegrationTest {

    @Autowired private TripUseCase trips;
    @Autowired private TripRepository tripRepository;
    @Autowired private DriverRepository drivers;
    @Autowired private DriverLicenseRepository licenses;
    @Autowired private DriverMedicalRecordRepository medicalRecords;
    @Autowired private DriverDrugTestRepository drugTests;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private final OffsetDateTime tripStart = OffsetDateTime.parse("2026-07-01T10:00:00Z");
    private final OffsetDateTime tripEnd = OffsetDateTime.parse("2026-07-01T14:00:00Z");

    @Test
    void rejectsTripDriverAssignmentWhenDriverMedicallyUnfit() {
        var driver = createDriverWithLicense();
        var trip = createTrip(tripStart, tripEnd);

        var medical = new DriverMedicalRecord(
                UUID.randomUUID(), driver.id(),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                DriverMedicalStatus.UNFIT,
                VisionTestStatus.FAILED,
                "Cardiac condition", "Dr. A", "MED-001", null,
                true, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        medicalRecords.save(medical);

        assertThatThrownBy(() -> trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MEDICALLY_UNFIT");
    }

    @Test
    void rejectsTripDriverAssignmentWhenDriverMedicalFitnessExpired() {
        var driver = createDriverWithLicense();
        var trip = createTrip(tripStart, tripEnd);

        var medical = new DriverMedicalRecord(
                UUID.randomUUID(), driver.id(),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 6, 1), // Expired before 2026-07-01
                DriverMedicalStatus.FIT,
                VisionTestStatus.PASSED,
                null, "Dr. A", "MED-002", null,
                true, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        medicalRecords.save(medical);

        assertThatThrownBy(() -> trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MEDICAL_FITNESS_EXPIRED");
    }

    @Test
    void rejectsTripDriverAssignmentWhenDriverHasPositiveUnclearedDrugTest() {
        var driver = createDriverWithLicense();
        var trip = createTrip(tripStart, tripEnd);

        var drugTest = new DriverDrugTest(
                UUID.randomUUID(), driver.id(),
                DrugTestType.RANDOM,
                LocalDate.of(2026, 6, 15),
                OffsetDateTime.now(ZoneOffset.UTC),
                LocalDate.of(2026, 6, 16),
                DrugTestResult.POSITIVE,
                DrugTestStatus.COMPLETED,
                "LabCorp", "REF-DT-01", "Positive THC",
                true, null, // Uncleared
                true, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin"
        );
        drugTests.save(drugTest);

        assertThatThrownBy(() -> trips.assignDriver(trip.id(), driver.id(), "B", "dispatcher"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RETURN_TO_DUTY_CLEARANCE_REQUIRED");
    }

    private Driver createDriverWithLicense() {
        var driverId = UUID.randomUUID();
        var driver = new Driver(driverId, "EMP-MED-" + driverId.toString().substring(0, 6), "Test", "Driver",
                "+1234567890", "test@example.com", "AVAILABLE", true);
        drivers.save(driver);

        var license = new DriverLicense(UUID.randomUUID(), driverId, "DL-" + UUID.randomUUID().toString().substring(0, 8),
                "B", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1),
                DriverLicenseStatus.ACTIVE, true, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), "admin", "admin");
        licenses.save(license);
        return driver;
    }

    private Trip createTrip(OffsetDateTime start, OffsetDateTime end) {
        var trip = new Trip(UUID.randomUUID(), "TRIP-MED-" + UUID.randomUUID().toString().substring(0, 6), null, null, null, null, "NORMAL", "APPROVED",
                UUID.randomUUID(), UUID.randomUUID(), start, end, null, null, null, null, null, null, null,
                null, null, null, null, null, null, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));
        tripLocations(jdbc, trip);
        tripRepository.save(trip);
        return trip;
    }
}
