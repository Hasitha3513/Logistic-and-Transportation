package com.transportlogistics.app.postgresql;

import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleMeterResetRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPriceUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseNumberGenerator;
import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelVoucherGenerator;
import com.transportlogistics.app.fuel.domain.model.FuelIssue;
import com.transportlogistics.app.fuel.domain.model.FuelIssueStatus;
import com.transportlogistics.app.fuel.domain.model.FuelPrice;
import com.transportlogistics.app.fuel.domain.model.FuelPurchase;
import com.transportlogistics.app.fuel.domain.model.FuelPurchaseStatus;
import com.transportlogistics.app.fuel.domain.model.FuelStation;
import com.transportlogistics.app.fuel.domain.model.FuelStationType;
import com.transportlogistics.app.fuel.domain.model.ReconciliationStatus;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripDispatchRepository;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.domain.model.Trip;
import com.transportlogistics.app.trip.domain.model.TripCommand;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.transportlogistics.app.support.ReferenceFixtures.tripLocations;
import static com.transportlogistics.app.support.ReferenceFixtures.vehicleHierarchy;
import static org.junit.jupiter.api.Assertions.*;

@Tag("postgres")
class PostgreSqlProductionInvariantIntegrationTest extends PostgreSqlIntegrationTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-16T00:00:00Z");

    @Autowired Flyway flyway;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired TripUseCase trips;
    @Autowired TripRepository tripRepository;
    @Autowired TripHistoryRepository tripHistory;
    @Autowired TripDispatchRepository dispatches;
    @Autowired VehicleRepository vehicles;
    @Autowired VehicleReadingUseCase vehicleReadings;
    @Autowired VehicleReadingRepository vehicleReadingRepository;
    @Autowired VehicleMeterResetRepository vehicleMeterResetRepository;
    @Autowired DriverRepository drivers;
    @Autowired DriverLicenseRepository licenses;
    @Autowired FuelIssueRepository fuelIssues;
    @Autowired FuelIssueUseCase fuelIssueUseCase;
    @Autowired FuelStationRepository stations;
    @Autowired FuelVoucherGenerator vouchers;
    @Autowired FuelPurchaseRepository purchases;
    @Autowired FuelPurchaseNumberGenerator purchaseNumbers;
    @Autowired FuelPriceRepository priceRepository;
    @Autowired FuelPriceUseCase fuelPrices;

    @Test
    void emptyPostgresqlAppliesEveryMigrationAndValidatesJpaSchema() {
        flyway.validate();
        var applied = List.of(flyway.info().applied());

        assertEquals(15, applied.size());
        assertEquals("15", applied.getLast().getVersion().getVersion());
        assertEquals("15", jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1",
                String.class));
        assertTrue(entityManagerFactory.isOpen());
        assertTrue(POSTGRES.isRunning());
        assertTrue(jdbc.queryForObject("SHOW server_version", String.class).startsWith("16.4"));
    }

    @Test
    void vehicleReadingIndexesAndPostgresqlSourceIdentityConstraintAreEnforced() {
        var indexNames = new HashSet<>(jdbc.queryForList("""
                SELECT indexname FROM pg_indexes WHERE schemaname = current_schema() AND tablename = 'vehicle_reading'
                """, String.class));
        assertTrue(indexNames.containsAll(List.of("idx_vehicle_reading_chronology", "idx_vehicle_reading_source",
                "idx_vehicle_reading_correction", "uq_vehicle_reading_idempotency",
                "uq_vehicle_reading_one_correction", "uq_vehicle_reading_source")));

        var actor = readingActor();
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        var sourceReference = UUID.randomUUID();
        vehicleReadingRepository.save(reading(vehicle.id(), actor, "1000", NOW, VehicleReadingSourceType.TRIP_START,
                sourceReference, "TRIP_START:" + sourceReference + ":ODOMETER"));

        assertThrows(DataIntegrityViolationException.class, () -> vehicleReadingRepository.save(reading(vehicle.id(),
                actor, "1000", NOW, VehicleReadingSourceType.TRIP_START, sourceReference, "different-key")));
    }

    @Test
    void concurrentVehicleReadingsSerializeAndPreserveChronology() throws Exception {
        var actor = readingActor();
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        vehicleReadings.record(readingCommand(vehicle.id(), actor, "10000", NOW.minusHours(3), "initial"));

        var results = race(
                () -> vehicleReadings.record(readingCommand(vehicle.id(), actor, "10200", NOW.minusHours(2), "race-a")),
                () -> vehicleReadings.record(readingCommand(vehicle.id(), actor, "10100", NOW.minusHours(1), "race-b")));

        assertEquals(1, results.stream().filter(VehicleReading.class::isInstance).count(), () -> results.toString());
        assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count(), () -> results.toString());
        var stored = vehicleReadingRepository.search(new VehicleReadingUseCase.SearchQuery(vehicle.id(),
                VehicleReadingType.ODOMETER, null, null, null, 0, 20)).content();
        assertEquals(2, stored.size());
        var chronological = stored.stream().sorted(java.util.Comparator.comparing(VehicleReading::recordedAt)).toList();
        assertTrue(chronological.get(0).value().compareTo(chronological.get(1).value()) <= 0);
    }

    @Test
    void rolledBackVehicleReadingLeavesNoFactOrSnapshotProjection() {
        var actor = readingActor();
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);

        assertThrows(IntentionalRollback.class, () -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    vehicleReadings.record(readingCommand(vehicle.id(), actor, "1100", NOW, "rollback-reading"));
                    throw new IntentionalRollback();
                }));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM vehicle_reading WHERE vehicle_id = ?",
                Long.class, vehicle.id()));
        assertEquals(1000d, vehicles.findById(vehicle.id()).orElseThrow().currentOdometerKm());
    }

    @Test
    void concurrentVehicleAssignmentsSerializeOnTheVehicleAndLeaveOneAllocation() throws Exception {
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        var start = OffsetDateTime.parse("2026-09-01T08:00:00Z");
        var first = approvedTrip("PG-VEH-A", start, start.plusHours(3));
        var second = approvedTrip("PG-VEH-B", start.plusHours(1), start.plusHours(4));
        saveTrip(first);
        saveTrip(second);

        var results = race(() -> trips.assignVehicle(first.id(), vehicle.id(), "postgres-race"),
                () -> trips.assignVehicle(second.id(), vehicle.id(), "postgres-race"));

        assertOneSuccessAndOneConflict(results);
        var persisted = List.of(trip(first.id()), trip(second.id()));
        assertEquals(1, persisted.stream().filter(value -> vehicle.id().equals(value.vehicleId())).count());
        assertEquals(1, actionCount(List.of(first.id(), second.id()), "VEHICLE_ASSIGNED"));
    }

    @Test
    void concurrentDriverAssignmentsSerializeOnTheDriverAndLeaveOneAssignment() throws Exception {
        var driver = driver();
        drivers.save(driver);
        licenses.save(license(driver.id()));
        var start = OffsetDateTime.parse("2026-09-02T08:00:00Z");
        var first = approvedTrip("PG-DRV-A", start, start.plusHours(3));
        var second = approvedTrip("PG-DRV-B", start.plusHours(1), start.plusHours(4));
        saveTrip(first);
        saveTrip(second);

        var results = race(() -> trips.assignDriver(first.id(), driver.id(), "B", "postgres-race"),
                () -> trips.assignDriver(second.id(), driver.id(), "B", "postgres-race"));

        assertOneSuccessAndOneConflict(results);
        var persisted = List.of(trip(first.id()), trip(second.id()));
        assertEquals(1, persisted.stream().filter(value -> driver.id().equals(value.driverId())).count());
        assertEquals(1, actionCount(List.of(first.id(), second.id()), "DRIVER_ASSIGNED"));
    }

    @Test
    void concurrentDispatchAndStartEachPersistExactlyOneLifecycleMutation() throws Exception {
        var vehicle = vehicle();
        var driver = driver();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        drivers.save(driver);
        licenses.save(license(driver.id()));
        readingActor(jdbc, "starter-a");
        readingActor(jdbc, "starter-b");
        var value = approvedTrip("PG-DISPATCH", OffsetDateTime.parse("2026-09-03T08:00:00Z"),
                OffsetDateTime.parse("2026-09-03T12:00:00Z"));
        saveTrip(value);
        trips.assignVehicle(value.id(), vehicle.id(), "allocator");
        trips.assignDriver(value.id(), driver.id(), "B", "allocator");

        var dispatchResults = race(() -> trips.dispatch(value.id(), "dispatcher-a", "PostgreSQL race"),
                () -> trips.dispatch(value.id(), "dispatcher-b", "PostgreSQL race"));
        assertOneSuccessAndOneConflict(dispatchResults);
        assertEquals("DISPATCHED", trip(value.id()).status());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM trip_dispatch WHERE trip_id = ?", Long.class,
                value.id()));
        assertEquals(1, actionCount(List.of(value.id()), "TRIP_DISPATCHED"));
        assertTrue(dispatches.findByTripId(value.id()).isPresent());

        var startResults = race(() -> trips.transition(value.id(), new TripCommand.Start(1000d), "starter-a"),
                () -> trips.transition(value.id(), new TripCommand.Start(1000d), "starter-b"));
        assertOneSuccessAndOneConflict(startResults);
        assertEquals("IN_PROGRESS", trip(value.id()).status());
        assertEquals(1, actionCount(List.of(value.id()), "TRIP_STARTED"));
    }

    @Test
    void tripStartReadingRejectionRollsBackTripLifecycleAtomicallyOnPostgresql() {
        var vehicle = vehicle();
        var driver = driver();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        drivers.save(driver);
        licenses.save(license(driver.id()));
        var starter = readingActor(jdbc, "pg-starter");

        // Seed an initial reading at 10,000 km
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("10000.000"),
                VehicleReadingSourceType.BASELINE, UUID.randomUUID(), NOW.minusHours(5), starter,
                "BASELINE:" + vehicle.id() + ":ODOMETER:V14", "baseline"
        ));

        var trip = approvedTrip("PG-ROLLBACK", OffsetDateTime.parse("2026-09-10T08:00:00Z"),
                OffsetDateTime.parse("2026-09-10T12:00:00Z"));
        saveTrip(trip);
        trips.assignVehicle(trip.id(), vehicle.id(), "allocator");
        trips.assignDriver(trip.id(), driver.id(), "B", "allocator");
        trips.dispatch(trip.id(), "dispatcher", "Gate 1");

        // Attempt start with a lower odometer reading (9,500 < 10,000)
        var error = assertThrows(ConflictException.class,
                () -> trips.transition(trip.id(), new TripCommand.Start(9500.0), "pg-starter"));
        assertEquals("VEHICLE_READING_DECREASE", error.code());

        // Verify full transactional atomicity: Trip remains DISPATCHED, no history entry, no reading row
        assertEquals("DISPATCHED", trip(trip.id()).status());
        assertEquals(0, actionCount(List.of(trip.id()), "TRIP_STARTED"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM vehicle_reading WHERE source_reference_id = ?", Long.class, trip.id()));
        assertEquals(10000.0, vehicles.findById(vehicle.id()).orElseThrow().currentOdometerKm());
    }

    @Test
    void multiTripAuthoritativeChronologySequenceEnforcedOnPostgresql() {
        var vehicle = vehicle();
        var driver = driver();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        drivers.save(driver);
        licenses.save(license(driver.id()));
        var actor = readingActor(jdbc, "pg-driver");

        // 1. Vehicle baseline 10,000 km
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("10000.000"),
                VehicleReadingSourceType.BASELINE, UUID.randomUUID(), NOW.minusDays(1), actor,
                "BASELINE:" + vehicle.id() + ":ODOMETER:V14", "baseline"
        ));

        // 2. Trip A: Start 10,010 km, Complete 10,100 km
        var tripA = approvedTrip("PG-TRIP-A", OffsetDateTime.parse("2026-09-11T08:00:00Z"),
                OffsetDateTime.parse("2026-09-11T12:00:00Z"));
        saveTrip(tripA);
        trips.assignVehicle(tripA.id(), vehicle.id(), "allocator");
        trips.assignDriver(tripA.id(), driver.id(), "B", "allocator");
        trips.dispatch(tripA.id(), "dispatcher", "Gate 1");
        trips.transition(tripA.id(), new TripCommand.Start(10010.0), "pg-driver");
        trips.transition(tripA.id(), new TripCommand.Complete(10100.0, "Delivered Trip A"), "pg-driver");

        // Verify Fleet reading ledger after Trip A
        var readings = vehicleReadingRepository.search(new VehicleReadingUseCase.SearchQuery(
                vehicle.id(), VehicleReadingType.ODOMETER, null, null, null, 0, 20)).content();
        var sources = readings.stream().sorted(java.util.Comparator.comparing(VehicleReading::recordedAt))
                .map(VehicleReading::sourceType).toList();
        assertEquals(List.of(VehicleReadingSourceType.BASELINE, VehicleReadingSourceType.TRIP_START,
                VehicleReadingSourceType.TRIP_END), sources);
        assertEquals(10100.0, vehicles.findById(vehicle.id()).orElseThrow().currentOdometerKm());

        // 3. Trip B: Attempt start at 10,050 km (after Trip A finished at 10,100 km)
        var tripB = approvedTrip("PG-TRIP-B", OffsetDateTime.parse("2026-09-12T08:00:00Z"),
                OffsetDateTime.parse("2026-09-12T12:00:00Z"));
        saveTrip(tripB);
        trips.assignVehicle(tripB.id(), vehicle.id(), "allocator");
        trips.assignDriver(tripB.id(), driver.id(), "B", "allocator");
        trips.dispatch(tripB.id(), "dispatcher", "Gate 2");

        var conflict = assertThrows(ConflictException.class,
                () -> trips.transition(tripB.id(), new TripCommand.Start(10050.0), "pg-driver"));
        assertEquals("VEHICLE_READING_DECREASE", conflict.code());
        assertEquals("DISPATCHED", trip(tripB.id()).status());
        assertEquals(0, actionCount(List.of(tripB.id()), "TRIP_STARTED"));
    }

    @Test
    void fuelIssueReadingRecordingAtomicityAndRollbackOnPostgresql() {
        var references = fuelReferences();
        var username = "pg-issuer-" + suffix();
        var actorId = readingActor(jdbc, username);

        // 1. Vehicle baseline 10,000 km, 50.0 engine hours
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                references.vehicleId(), VehicleReadingType.ODOMETER, new BigDecimal("10000.000"),
                VehicleReadingSourceType.BASELINE, UUID.randomUUID(), NOW.minusDays(1), actorId,
                "BASELINE:" + references.vehicleId() + ":ODOMETER:V14", "baseline"
        ));

        // 2. Create and authorize Fuel Issue with lower odometer (9,500 < 10,000)
        var draft = fuelIssueUseCase.create(new FuelIssueUseCase.CreateCommand(
                references.vehicleId(), null, null, "DIESEL", new BigDecimal("40"), new BigDecimal("350.00"),
                references.stationId(), new BigDecimal("9500.000"), new BigDecimal("60.000"), NOW, "Fuel issue test"
        ), username);
        fuelIssueUseCase.submit(draft.id(), username);
        fuelIssueUseCase.authorize(draft.id(), "Approved", username);

        // 3. Attempt issue transition -> must fail due to VEHICLE_READING_DECREASE
        var conflict = assertThrows(ConflictException.class,
                () -> fuelIssueUseCase.issue(draft.id(), username));
        assertEquals("VEHICLE_READING_DECREASE", conflict.code());

        // 4. Verify transaction rollback: Fuel Issue remains AUTHORIZED, no ISSUED history, no reading rows, current odometer unaffected
        var currentIssue = fuelIssues.findById(draft.id()).orElseThrow();
        assertEquals(FuelIssueStatus.AUTHORIZED, currentIssue.status());
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM fuel_issue_history WHERE fuel_issue_id = ? AND action = 'ISSUED'",
                Long.class, draft.id()));
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM vehicle_reading WHERE source_reference_id = ?",
                Long.class, draft.id()));
        assertEquals(10000.0, vehicles.findById(references.vehicleId()).orElseThrow().currentOdometerKm());
    }

    @Test
    void backdatedFuelIssueChronologyValidityAndRejectionOnPostgresql() {
        var references = fuelReferences();
        var username = "pg-backdate-" + suffix();
        var actorId = readingActor(jdbc, username);

        // Aug 10: 10,000 km
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                references.vehicleId(), VehicleReadingType.ODOMETER, new BigDecimal("10000.000"),
                VehicleReadingSourceType.BASELINE, UUID.randomUUID(), OffsetDateTime.parse("2026-08-10T00:00:00Z"),
                actorId, "BASELINE:" + references.vehicleId() + ":ODOMETER:V14", "baseline"
        ));
        // Aug 12: 10,200 km
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                references.vehicleId(), VehicleReadingType.ODOMETER, new BigDecimal("10200.000"),
                VehicleReadingSourceType.MANUAL, null, OffsetDateTime.parse("2026-08-12T00:00:00Z"),
                actorId, "MANUAL:" + references.vehicleId() + ":ODOMETER:10200", "manual reading"
        ));

        // Aug 11 with 10,100 km -> VALID
        var validIssue = fuelIssueUseCase.create(new FuelIssueUseCase.CreateCommand(
                references.vehicleId(), null, null, "DIESEL", new BigDecimal("30"), new BigDecimal("350.00"),
                references.stationId(), new BigDecimal("10100.000"), null, OffsetDateTime.parse("2026-08-11T12:00:00Z"), "Backdated valid"
        ), username);
        fuelIssueUseCase.submit(validIssue.id(), username);
        fuelIssueUseCase.authorize(validIssue.id(), "Approved", username);
        var issuedValid = fuelIssueUseCase.issue(validIssue.id(), username);
        assertEquals(FuelIssueStatus.ISSUED, issuedValid.status());

        // Aug 11 with 10,500 km -> REJECTED (chronology conflict with Aug 12 10,200 km)
        var invalidIssue = fuelIssueUseCase.create(new FuelIssueUseCase.CreateCommand(
                references.vehicleId(), null, null, "DIESEL", new BigDecimal("30"), new BigDecimal("350.00"),
                references.stationId(), new BigDecimal("10500.000"), null, OffsetDateTime.parse("2026-08-11T14:00:00Z"), "Backdated invalid"
        ), username);
        fuelIssueUseCase.submit(invalidIssue.id(), username);
        fuelIssueUseCase.authorize(invalidIssue.id(), "Approved", username);
        var conflict = assertThrows(ConflictException.class,
                () -> fuelIssueUseCase.issue(invalidIssue.id(), username));
        assertEquals("VEHICLE_READING_CHRONOLOGY_CONFLICT", conflict.code());
        assertEquals(FuelIssueStatus.AUTHORIZED, fuelIssues.findById(invalidIssue.id()).orElseThrow().status());
    }

    @Test
    void tripAndFuelSharedLedgerChronologyIntegrationOnPostgresql() throws Exception {
        var vehicle = vehicle();
        var driver = driver();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        drivers.save(driver);
        licenses.save(license(driver.id()));
        var stationId = UUID.randomUUID();
        stations.save(new FuelStation(stationId, "PG-ST-" + suffix(), "Depot", FuelStationType.INTERNAL, true, null, null));
        var username = "pg-shared-" + suffix();
        var actorId = readingActor(jdbc, username);

        // 1. Vehicle baseline: 10,000 km (recorded 2 hours ago)
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("10000.000"),
                VehicleReadingSourceType.BASELINE, UUID.randomUUID(), OffsetDateTime.now().minusHours(2),
                actorId, "BASELINE:" + vehicle.id() + ":ODOMETER:V14", "baseline"
        ));

        // 2. Trip Start at 10,000 km
        var trip = approvedTrip("PG-TRIP-FUEL", OffsetDateTime.now().minusMinutes(30),
                OffsetDateTime.now().plusHours(4));
        saveTrip(trip);
        trips.assignVehicle(trip.id(), vehicle.id(), "allocator");
        trips.assignDriver(trip.id(), driver.id(), "B", "allocator");
        trips.dispatch(trip.id(), "dispatcher", "Gate 1");
        trips.transition(trip.id(), new TripCommand.Start(10000.0, 50.0), username);

        Thread.sleep(50);

        // 3. Fuel Issue during trip at 10,050 km, engine hours 52.5
        var fuelIssue = fuelIssueUseCase.create(new FuelIssueUseCase.CreateCommand(
                vehicle.id(), trip.id(), driver.id(), "DIESEL", new BigDecimal("50"), new BigDecimal("350.00"),
                stationId, new BigDecimal("10050.000"), new BigDecimal("52.500"), OffsetDateTime.now(), "Trip refueling"
        ), username);
        fuelIssueUseCase.submit(fuelIssue.id(), username);
        fuelIssueUseCase.authorize(fuelIssue.id(), "Approved", username);
        fuelIssueUseCase.issue(fuelIssue.id(), username);

        Thread.sleep(50);

        // 4. Trip Complete at 10,100 km
        trips.transition(trip.id(), new TripCommand.Complete(10100.0, "Delivered successfully", 55.0), username);

        // 5. Inspect single Fleet-owned VehicleReading ledger
        var readings = vehicleReadingRepository.search(new VehicleReadingUseCase.SearchQuery(
                vehicle.id(), VehicleReadingType.ODOMETER, null, null, null, 0, 20)).content();
        var sortedOdometerReadings = readings.stream()
                .sorted(java.util.Comparator.comparing(VehicleReading::recordedAt))
                .toList();

        assertEquals(4, sortedOdometerReadings.size());
        assertEquals(VehicleReadingSourceType.BASELINE, sortedOdometerReadings.get(0).sourceType());
        assertEquals(0, new BigDecimal("10000.000").compareTo(sortedOdometerReadings.get(0).value()));

        assertEquals(VehicleReadingSourceType.TRIP_START, sortedOdometerReadings.get(1).sourceType());
        assertEquals(trip.id(), sortedOdometerReadings.get(1).sourceReferenceId());
        assertEquals(0, new BigDecimal("10000.000").compareTo(sortedOdometerReadings.get(1).value()));

        assertEquals(VehicleReadingSourceType.FUEL_ISSUE, sortedOdometerReadings.get(2).sourceType());
        assertEquals(fuelIssue.id(), sortedOdometerReadings.get(2).sourceReferenceId());
        assertEquals(0, new BigDecimal("10050.000").compareTo(sortedOdometerReadings.get(2).value()));

        assertEquals(VehicleReadingSourceType.TRIP_END, sortedOdometerReadings.get(3).sourceType());
        assertEquals(trip.id(), sortedOdometerReadings.get(3).sourceReferenceId());
        assertEquals(0, new BigDecimal("10100.000").compareTo(sortedOdometerReadings.get(3).value()));

        // Also check engine hours recorded for FUEL_ISSUE
        var engineHoursReadings = vehicleReadingRepository.search(new VehicleReadingUseCase.SearchQuery(
                vehicle.id(), VehicleReadingType.ENGINE_HOURS, null, null, null, 0, 20)).content();
        assertTrue(engineHoursReadings.stream().anyMatch(r -> r.sourceType() == VehicleReadingSourceType.FUEL_ISSUE
                && r.sourceReferenceId().equals(fuelIssue.id())
                && new BigDecimal("52.500").compareTo(r.value()) == 0));

        // Vehicle projected snapshot is up to date with the latest reading
        var updatedVehicle = vehicles.findById(vehicle.id()).orElseThrow();
        assertEquals(10100.0, updatedVehicle.currentOdometerKm());
        assertEquals(55.0, updatedVehicle.engineHours());
    }

    @Test
    void concurrentFuelIssueCreationUsesUniqueVouchersAndSequenceSurvivesRollback() throws Exception {
        var references = fuelReferences();
        var created = concurrent(12, index -> {
            var voucher = vouchers.next(2026);
            return fuelIssues.save(issue(UUID.randomUUID(), voucher, references));
        });

        assertEquals(12, created.size());
        assertEquals(12, created.stream().map(FuelIssue::voucherNumber).collect(java.util.stream.Collectors.toSet()).size());
        var duplicate = created.getFirst();
        assertThrows(RuntimeException.class, () -> fuelIssues.save(issue(UUID.randomUUID(),
                duplicate.voucherNumber(), references)));

        var rolledBack = new String[1];
        assertThrows(IntentionalRollback.class, () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            rolledBack[0] = vouchers.next(2026);
            throw new IntentionalRollback();
        }));
        var afterRollback = vouchers.next(2026);
        assertNotEquals(rolledBack[0], afterRollback);
        assertTrue(sequencePart(afterRollback) > sequencePart(rolledBack[0]));
    }

    @Test
    void concurrentFuelPurchaseCreationUsesUniqueNumbersAndSequenceSurvivesRollback() throws Exception {
        var references = fuelReferences();
        var created = concurrent(12, index -> {
            var number = purchaseNumbers.next(LocalDate.of(2026, 8, 16));
            return purchases.save(purchase(UUID.randomUUID(), number, "CONCURRENT-" + index, references));
        });

        assertEquals(12, created.size());
        assertEquals(12, created.stream().map(FuelPurchase::purchaseNumber)
                .collect(java.util.stream.Collectors.toSet()).size());
        var duplicateNumber = created.getFirst().purchaseNumber();
        assertThrows(RuntimeException.class, () -> purchases.save(purchase(UUID.randomUUID(), duplicateNumber,
                "DUPLICATE-NUMBER", references)));

        var rolledBack = new String[1];
        assertThrows(IntentionalRollback.class, () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            rolledBack[0] = purchaseNumbers.next(LocalDate.of(2026, 8, 16));
            throw new IntentionalRollback();
        }));
        var afterRollback = purchaseNumbers.next(LocalDate.of(2026, 8, 16));
        assertNotEquals(rolledBack[0], afterRollback);
        assertTrue(sequencePart(afterRollback) > sequencePart(rolledBack[0]));
    }

    @Test
    void vendorInvoiceConstraintIsCaseSensitiveWhileApplicationLookupIsCaseInsensitive() {
        var references = fuelReferences();
        purchases.save(purchase(UUID.randomUUID(), purchaseNumbers.next(LocalDate.now()), "INV-001", references));

        assertThrows(RuntimeException.class, () -> purchases.save(purchase(UUID.randomUUID(),
                purchaseNumbers.next(LocalDate.now()), "INV-001", references)));
        assertTrue(purchases.existsByVendorAndInvoice(references.vendorId(), "inv-001", null));
        assertDoesNotThrow(() -> purchases.save(purchase(UUID.randomUUID(), purchaseNumbers.next(LocalDate.now()),
                "inv-001", references)));
    }

    @Test
    void fuelPriceOverlapIsApplicationEnforcedAndAdjacentPeriodsAreAllowed() {
        var references = fuelReferences();
        fuelPrices.create(new FuelPriceUseCase.Command(references.vendorId(), "DIESEL",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), BigDecimal.TEN, "LKR", true));

        assertThrows(ConflictException.class, () -> fuelPrices.create(new FuelPriceUseCase.Command(
                references.vendorId(), "DIESEL", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 12, 31),
                BigDecimal.ONE, "LKR", true)));
        assertDoesNotThrow(() -> fuelPrices.create(new FuelPriceUseCase.Command(references.vendorId(), "DIESEL",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), BigDecimal.ONE, "LKR", true)));

        assertDoesNotThrow(() -> priceRepository.save(new FuelPrice(UUID.randomUUID(), references.vendorId(),
                "DIESEL", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), new BigDecimal("2"), "LKR", true,
                NOW, NOW)));
    }

    @Test
    void criticalForeignKeysExistAndRejectInvalidTripAndFuelReferences() {
        var constraints = new HashSet<>(jdbc.queryForList(
                "SELECT conname FROM pg_constraint WHERE contype = 'f'", String.class));
        assertTrue(constraints.containsAll(List.of(
                "fk_trip_customer", "fk_trip_department", "fk_trip_project", "fk_trip_route",
                "fk_trip_origin", "fk_trip_destination", "fk_trip_required_vehicle_type", "fk_trip_vehicle",
                "fk_trip_driver", "fk_fuel_issue_vehicle", "fk_fuel_issue_trip", "fk_fuel_issue_driver",
                "fk_fuel_issue_station", "fk_fuel_limit_vehicle", "fk_fuel_purchase_vendor",
                "fk_fuel_purchase_station", "fk_fuel_purchase_destination", "fk_fuel_purchase_history_purchase")));

        var value = approvedTrip("PG-FK", OffsetDateTime.parse("2026-09-04T08:00:00Z"),
                OffsetDateTime.parse("2026-09-04T12:00:00Z"));
        saveTrip(value);
        for (var column : List.of("customer_id", "department_id", "project_id", "route_id", "vehicle_id", "driver_id")) {
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("UPDATE trip SET " + column + " = ? WHERE id = ?", UUID.randomUUID(), value.id()));
        }
        assertNull(trip(value.id()).vehicleId());
        assertNull(trip(value.id()).driverId());

        var references = fuelReferences();
        var invalidIssue = new FuelIssue(UUID.randomUUID(), vouchers.next(2026), references.vehicleId(), null, null,
                "DIESEL", BigDecimal.ONE, null, null, UUID.randomUUID(), null, null, NOW,
                FuelIssueStatus.DRAFT, references.userId(), null, null, null, NOW, NOW);
        assertThrows(RuntimeException.class, () -> fuelIssues.save(invalidIssue));
        assertThrows(RuntimeException.class, () -> purchases.save(purchase(UUID.randomUUID(),
                purchaseNumbers.next(LocalDate.now()), "INVALID-VENDOR", references, UUID.randomUUID())));
    }

    @Test
    void failedAuditWriteRollsBackTheFuelPurchaseWrittenEarlierInTheTransaction() {
        var references = fuelReferences();
        var value = purchase(UUID.randomUUID(), purchaseNumbers.next(LocalDate.now()), "ROLLBACK-INV", references);

        assertThrows(DataIntegrityViolationException.class,
                () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    purchases.save(value);
                    jdbc.update("""
                            INSERT INTO fuel_purchase_history
                                (id, fuel_purchase_id, from_status, to_status, action, actor_id, actor, occurred_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """, UUID.randomUUID(), value.id(), null, "DRAFT", "CREATED", UUID.randomUUID(),
                            "invalid-actor", NOW);
                }));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM fuel_purchase WHERE id = ?", Long.class, value.id()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM fuel_purchase_history WHERE fuel_purchase_id = ?",
                Long.class, value.id()));
    }

    @Test
    void postgresqlUniqueConstraintsProtectBusinessNumbersPermissionsAndRouteStops() {
        var tripNumber = "PG-UNIQUE-" + suffix();
        var first = approvedTrip(tripNumber, OffsetDateTime.parse("2026-09-05T08:00:00Z"),
                OffsetDateTime.parse("2026-09-05T12:00:00Z"));
        saveTrip(first);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO trip
                    (id, trip_number, priority, status, origin_location_id, destination_location_id,
                     requested_start_time, requested_end_time, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), first.tripNumber(), "NORMAL", "APPROVED", first.originLocationId(),
                first.destinationLocationId(), OffsetDateTime.parse("2026-09-06T08:00:00Z"),
                OffsetDateTime.parse("2026-09-06T12:00:00Z"), NOW, NOW));

        var roleId = UUID.randomUUID();
        jdbc.update("INSERT INTO app_role (id, name, active) VALUES (?, ?, ?)", roleId, "PG-ROLE-" + suffix(), true);
        jdbc.update("INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, ?)", roleId, "CUSTOMER_VIEW");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO app_role_permission (role_id, permission_code) VALUES (?, ?)", roleId, "CUSTOMER_VIEW"));

        var routeId = UUID.randomUUID();
        var origin = UUID.randomUUID();
        var destination = UUID.randomUUID();
        tripLocations(jdbc, approvedTripWithLocations("IGNORED", origin, destination));
        jdbc.update("INSERT INTO route (id, code, name, origin_location_id, destination_location_id, active) VALUES (?, ?, ?, ?, ?, ?)",
                routeId, "PG-ROUTE-" + suffix(), "PostgreSQL route", origin, destination, true);
        jdbc.update("INSERT INTO route_stop (route_id, location_id, stop_order) VALUES (?, ?, ?)",
                routeId, origin, 1);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO route_stop (route_id, location_id, stop_order) VALUES (?, ?, ?)",
                routeId, origin, 2));
    }

    private void saveTrip(Trip value) {
        tripLocations(jdbc, value);
        tripRepository.save(value);
    }

    private Trip trip(UUID id) {
        return tripRepository.findById(id).orElseThrow();
    }

    private long actionCount(List<UUID> tripIds, String action) {
        return tripIds.stream().flatMap(id -> tripHistory.findByTripId(id).stream())
                .filter(entry -> action.equals(entry.action())).count();
    }

    private void assertOneSuccessAndOneConflict(List<Object> results) {
        assertEquals(1, results.stream().filter(Trip.class::isInstance).count(), () -> results.toString());
        assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count(), () -> results.toString());
    }

    private <T> List<Object> race(Supplier<T> first, Supplier<T> second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Object> firstTask = () -> raced(first, ready, start);
            Callable<Object> secondTask = () -> raced(second, ready, start);
            var firstResult = executor.submit(firstTask);
            var secondResult = executor.submit(secondTask);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return List.of(firstResult.get(20, TimeUnit.SECONDS), secondResult.get(20, TimeUnit.SECONDS));
        }
    }

    private <T> Object raced(Supplier<T> action, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return action.get();
        } catch (RuntimeException exception) {
            return exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return exception;
        }
    }

    private <T> List<T> concurrent(int count, IndexedOperation<T> operation) throws Exception {
        var ready = new CountDownLatch(count);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(count)) {
            var futures = java.util.stream.IntStream.range(0, count).mapToObj(index -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Race did not start");
                return operation.run(index);
            })).toList();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var values = new java.util.ArrayList<T>();
            for (var future : futures) values.add(future.get(30, TimeUnit.SECONDS));
            return values;
        }
    }

    private Vehicle vehicle() {
        return new Vehicle(UUID.randomUUID(), "PG-REG-" + suffix(), null, null, UUID.randomUUID(), UUID.randomUUID(),
                "Maker", "Model", 2026, "OWNED", "AVAILABLE", 1000d, null, 5000d, true);
    }

    private VehicleReadingUseCase.RecordCommand readingCommand(UUID vehicleId, UUID actor, String value,
                                                                OffsetDateTime at, String key) {
        return new VehicleReadingUseCase.RecordCommand(vehicleId, VehicleReadingType.ODOMETER,
                new BigDecimal(value), VehicleReadingSourceType.MANUAL, null, at, actor, key, "postgres test");
    }

    private VehicleReading reading(UUID vehicleId, UUID actor, String value, OffsetDateTime at,
                                   VehicleReadingSourceType source, UUID sourceReference, String key) {
        return new VehicleReading(UUID.randomUUID(), vehicleId, VehicleReadingType.ODOMETER,
                new BigDecimal(value).setScale(3), VehicleReadingType.ODOMETER.unit(), 0, source, sourceReference,
                at, NOW, actor, null, null, key, null, NOW);
    }

    private UUID readingActor() {
        return readingActor(jdbc, "reading-user-" + UUID.randomUUID());
    }

    private UUID readingActor(JdbcTemplate jdbc, String username) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, username, username + "@test.local", "unused", "Reading", "Tester", true, NOW, NOW);
        return id;
    }

    private Driver driver() {
        return new Driver(UUID.randomUUID(), "PG-EMP-" + suffix(), "Postgres", "Driver", null, null,
                "AVAILABLE", true);
    }

    private DriverLicense license(UUID driverId) {
        return new DriverLicense(UUID.randomUUID(), driverId, "PG-DL-" + suffix(), "B",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 12, 31), DriverLicenseStatus.ACTIVE, true,
                NOW, NOW, "postgres-test", "postgres-test");
    }

    private Trip approvedTrip(String prefix, OffsetDateTime start, OffsetDateTime end) {
        return approvedTripWithLocations(prefix + "-" + suffix(), UUID.randomUUID(), UUID.randomUUID(), start, end);
    }

    private Trip approvedTripWithLocations(String number, UUID origin, UUID destination) {
        return approvedTripWithLocations(number, origin, destination, NOW.plusDays(1), NOW.plusDays(2));
    }

    private Trip approvedTripWithLocations(String number, UUID origin, UUID destination,
                                           OffsetDateTime start, OffsetDateTime end) {
        return new Trip(UUID.randomUUID(), number, null, null, null, null, "NORMAL", "APPROVED", origin,
                destination, start, end, null, null, null, null, null, null, null, null, null, null, null, null,
                null, NOW, NOW);
    }

    private FuelReferences fuelReferences() {
        var userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, "pg-user-" + userId, userId + "@test.local", "not-used", "Postgres", "Tester",
                true, NOW, NOW);
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        var stationId = UUID.randomUUID();
        stations.save(new FuelStation(stationId, "PG-ST-" + suffix(), "PostgreSQL Station",
                FuelStationType.INTERNAL, true, null, null));
        var vendorId = UUID.randomUUID();
        jdbc.update("INSERT INTO vendor (id, code, name, active) VALUES (?, ?, ?, ?)", vendorId,
                "PG-VENDOR-" + suffix(), "PostgreSQL Vendor", true);
        return new FuelReferences(userId, vehicle.id(), stationId, vendorId);
    }

    private FuelIssue issue(UUID id, String voucher, FuelReferences references) {
        return new FuelIssue(id, voucher, references.vehicleId(), null, null, "DIESEL", BigDecimal.ONE,
                null, null, references.stationId(), null, null, NOW, FuelIssueStatus.DRAFT, references.userId(),
                null, null, null, NOW, NOW);
    }

    private FuelPurchase purchase(UUID id, String number, String invoice, FuelReferences references) {
        return purchase(id, number, invoice, references, references.vendorId());
    }

    private FuelPurchase purchase(UUID id, String number, String invoice, FuelReferences references, UUID vendorId) {
        return new FuelPurchase(id, number, vendorId, null, "DIESEL", LocalDate.of(2026, 8, 16), invoice,
                LocalDate.of(2026, 8, 16), BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("100"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"), "LKR", FuelPurchaseStatus.DRAFT,
                ReconciliationStatus.PENDING, null, null, null, null, null, null, null, null, null, null, null,
                null, null, "PostgreSQL test", references.userId(), NOW, NOW);
    }

    private long sequencePart(String value) {
        return Long.parseLong(value.substring(value.lastIndexOf('-') + 1));
    }

    @Test
    void vehicleReadingCorrectionAndMeterResetInvariantsAreEnforcedInPostgreSql() {
        var indexNames = new HashSet<>(jdbc.queryForList("""
                SELECT indexname FROM pg_indexes WHERE schemaname = current_schema() AND tablename = 'vehicle_meter_reset'
                """, String.class));
        assertTrue(indexNames.containsAll(List.of("idx_vehicle_meter_reset_vehicle", "idx_vehicle_meter_reset_new_reading")));

        var actor = readingActor();
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);

        var r1 = vehicleReadingRepository.save(reading(vehicle.id(), actor, "10000", NOW.minusDays(2),
                VehicleReadingSourceType.MANUAL, null, "orig-1"));

        // First correction succeeds
        var corr1 = vehicleReadingRepository.save(new VehicleReading(UUID.randomUUID(), vehicle.id(),
                VehicleReadingType.ODOMETER, new BigDecimal("10050.000"), VehicleReadingType.ODOMETER.unit(),
                0, VehicleReadingSourceType.MANUAL, null, NOW.minusDays(2), NOW, actor, r1.id(), "Typo fix",
                "corr-key-1", "Fixed", NOW));
        assertNotNull(corr1.id());

        // Second parallel correction targeting r1 fails DB unique constraint uq_vehicle_reading_one_correction
        assertThrows(DataIntegrityViolationException.class, () -> vehicleReadingRepository.save(new VehicleReading(
                UUID.randomUUID(), vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("10060.000"),
                VehicleReadingType.ODOMETER.unit(), 0, VehicleReadingSourceType.MANUAL, null, NOW.minusDays(2),
                NOW, actor, r1.id(), "Second fix", "corr-key-2", null, NOW)));

        // Self correction fails DB check constraint chk_vehicle_reading_no_self_correction
        var selfId = UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class, () -> vehicleReadingRepository.save(new VehicleReading(
                selfId, vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("10070.000"),
                VehicleReadingType.ODOMETER.unit(), 0, VehicleReadingSourceType.MANUAL, null, NOW.minusDays(2),
                NOW, actor, selfId, "Self correction", "self-key", null, NOW)));

        // Meter reset saves successfully to vehicle_meter_reset table
        var reset = vehicleMeterResetRepository.save(new VehicleMeterReset(UUID.randomUUID(), vehicle.id(),
                VehicleReadingType.ODOMETER, r1.id(), r1.value(), corr1.id(), corr1.value(), NOW,
                "Cluster replaced", actor, actor, "Work order 456", NOW));
        assertNotNull(reset.id());
        var foundReset = vehicleMeterResetRepository.findById(reset.id());
        assertTrue(foundReset.isPresent());
        assertEquals("Cluster replaced", foundReset.get().reason());
    }

    @Test
    void realisticMultiEpochVehicleReadingAndMeterResetScenario() {
        var actor = readingActor();
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);

        // Epoch 0: Initial baseline 245,000 km
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("245000.000"),
                VehicleReadingSourceType.BASELINE, UUID.randomUUID(), NOW.minusDays(10), actor, null, "Baseline"));

        // Epoch 0: Trip Start at 245,000 km
        var trip1Id = UUID.randomUUID();
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("245000.000"),
                VehicleReadingSourceType.TRIP_START, trip1Id, NOW.minusDays(8), actor, null, "Trip 1 start"));

        // Epoch 0: Trip End at 245,100 km
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("245100.000"),
                VehicleReadingSourceType.TRIP_END, trip1Id, NOW.minusDays(7), actor, null, "Trip 1 end"));

        assertEquals(245100d, vehicles.findById(vehicle.id()).orElseThrow().currentOdometerKm());

        // Meter replacement event: Meter Reset to 0 km -> Epoch 1
        var reset = vehicleReadings.resetMeter(new VehicleReadingUseCase.ResetMeterCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, BigDecimal.ZERO, NOW.minusDays(5),
                "Physical odometer replacement after hardware failure", actor, actor, "WO-9988"));

        assertEquals(0, BigDecimal.ZERO.setScale(3).compareTo(reset.newMeterValue()));
        assertEquals(1, vehicleReadingRepository.findCurrentMeterEpoch(vehicle.id(), VehicleReadingType.ODOMETER));
        assertEquals(0d, vehicles.findById(vehicle.id()).orElseThrow().currentOdometerKm());

        // Epoch 1: Fuel Issue at 50 km
        var fuelIssueId = UUID.randomUUID();
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("50.000"),
                VehicleReadingSourceType.FUEL_ISSUE, fuelIssueId, NOW.minusDays(3), actor, null, "Fuel fill-up"));

        // Epoch 1: Trip 2 Start at 80 km, End at 120 km
        var trip2Id = UUID.randomUUID();
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("80.000"),
                VehicleReadingSourceType.TRIP_START, trip2Id, NOW.minusDays(2), actor, null, "Trip 2 start"));
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("120.000"),
                VehicleReadingSourceType.TRIP_END, trip2Id, NOW.minusDays(1), actor, null, "Trip 2 end"));

        // Verify latest snapshot and effective reading in epoch 1
        assertEquals(120d, vehicles.findById(vehicle.id()).orElseThrow().currentOdometerKm());
        var latest = vehicleReadings.latest(vehicle.id()).odometer().orElseThrow();
        assertEquals(new BigDecimal("120.000"), latest.value());
        assertEquals(1, latest.meterEpoch());

        // Verify history contains all 7 readings across both epochs (3 in epoch 0, 4 in epoch 1 including METER_RESET baseline)
        var allReadings = vehicleReadingRepository.search(new VehicleReadingUseCase.SearchQuery(
                vehicle.id(), VehicleReadingType.ODOMETER, null, null, null, 0, 50)).content();
        assertEquals(7, allReadings.size());

        // Verify epoch 0 readings remain untouched
        var epoch0Readings = allReadings.stream().filter(r -> r.meterEpoch() == 0).toList();
        assertEquals(3, epoch0Readings.size());

        // Verify epoch 1 readings
        var epoch1Readings = allReadings.stream().filter(r -> r.meterEpoch() == 1).toList();
        assertEquals(4, epoch1Readings.size());
    }

    @Test
    void postgresqlAuthoritativeMileageAndTripDistanceCalculation() {
        var vehicle = vehicle();
        vehicleHierarchy(jdbc, vehicle);
        vehicles.save(vehicle);
        var actor = readingActor();

        var t0 = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        var tPreReset = OffsetDateTime.parse("2026-08-05T12:00:00Z");
        var tReset = OffsetDateTime.parse("2026-08-05T14:00:00Z");
        var tPostReset = OffsetDateTime.parse("2026-08-10T10:00:00Z");

        // 1. Multi-epoch scenario: 245,000 -> 245,500 (500 km), Reset to 0, 0 -> 800 (800 km) = 1,300 km total
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("245000.000"),
                VehicleReadingSourceType.BASELINE, null, t0, actor, null, "Baseline"));
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("245500.000"),
                VehicleReadingSourceType.MANUAL, null, tPreReset, actor, null, "Pre-reset"));
        vehicleReadings.resetMeter(new VehicleReadingUseCase.ResetMeterCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, BigDecimal.ZERO, tReset,
                "Cluster replaced", actor, actor, "Reset note"));
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                vehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("800.000"),
                VehicleReadingSourceType.MANUAL, null, tPostReset, actor, null, "Post-reset"));

        var summary = vehicleReadings.mileageSummary(vehicle.id(),
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-15T00:00:00Z"),
                true);

        assertEquals(0, new BigDecimal("245000.000").compareTo(summary.openingOdometer()));
        assertEquals(0, new BigDecimal("800.000").compareTo(summary.closingOdometer()));
        assertEquals(0, new BigDecimal("1300.000").compareTo(summary.distanceKm()));
        assertEquals(com.transportlogistics.app.fleet.CoverageStatus.COMPLETE, summary.coverageStatus());
        assertEquals(1, summary.meterResetCount());

        // 2. Trip distance calculation from VehicleReading ledger
        var tripVehicle = vehicle();
        vehicleHierarchy(jdbc, tripVehicle);
        vehicles.save(tripVehicle);

        var tripId = UUID.randomUUID();
        var tTripStart = OffsetDateTime.parse("2026-08-12T08:00:00Z");
        var tTripFuel = OffsetDateTime.parse("2026-08-12T11:00:00Z");
        var tTripEnd = OffsetDateTime.parse("2026-08-12T16:00:00Z");

        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                tripVehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("50000.000"),
                VehicleReadingSourceType.TRIP_START, tripId, tTripStart, actor, null, "Trip start"));
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                tripVehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("50150.000"),
                VehicleReadingSourceType.FUEL_ISSUE, UUID.randomUUID(), tTripFuel, actor, null, "En route fuel"));
        vehicleReadings.record(new VehicleReadingUseCase.RecordCommand(
                tripVehicle.id(), VehicleReadingType.ODOMETER, new BigDecimal("50320.000"),
                VehicleReadingSourceType.TRIP_END, tripId, tTripEnd, actor, null, "Trip end"));

        var tripDist = vehicleReadings.tripDistance(tripId, tripVehicle.id());
        assertEquals(0, new BigDecimal("320.000").compareTo(tripDist.distanceKm()));
        assertEquals(com.transportlogistics.app.fleet.TripDistanceStatus.AVAILABLE, tripDist.status());
        assertEquals(0, new BigDecimal("50000.000").compareTo(tripDist.startOdometer()));
        assertEquals(0, new BigDecimal("50320.000").compareTo(tripDist.endOdometer()));
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record FuelReferences(UUID userId, UUID vehicleId, UUID stationId, UUID vendorId) {
    }

    @FunctionalInterface
    private interface IndexedOperation<T> {
        T run(int index) throws Exception;
    }

    private static final class IntentionalRollback extends RuntimeException {
    }
}
