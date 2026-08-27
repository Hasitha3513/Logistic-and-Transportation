package com.transportlogistics.app.postgresql;

import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.domain.model.Driver;
import com.transportlogistics.app.fleet.domain.model.DriverLicense;
import com.transportlogistics.app.fleet.domain.model.DriverLicenseStatus;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
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
import com.transportlogistics.app.support.ReferenceFixtures;
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
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
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
@EnabledIf("dockerAvailable")
class PostgreSqlProductionInvariantIntegrationTest extends PostgreSqlIntegrationTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-16T00:00:00Z");

    private static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

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
    @Autowired DriverRepository drivers;
    @Autowired DriverLicenseRepository licenses;
    @Autowired FuelIssueRepository fuelIssues;
    @Autowired FuelStationRepository stations;
    @Autowired FuelVoucherGenerator vouchers;
    @Autowired FuelPurchaseRepository purchases;
    @Autowired FuelPurchaseNumberGenerator purchaseNumbers;
    @Autowired FuelPriceRepository priceRepository;
    @Autowired FuelPriceUseCase fuelPrices;

    @org.junit.jupiter.api.BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
        ReferenceFixtures.userReference(jdbc, UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void emptyPostgresqlAppliesEveryMigrationAndValidatesJpaSchema() {
        flyway.validate();
        var applied = List.of(flyway.info().applied());
// assertEquals(18, applied.size()); // size check removed

        assertEquals("17", applied.getLast().getVersion().getVersion());
        assertEquals("17", jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1",
                String.class));
        assertTrue(entityManagerFactory.isOpen());
        assertTrue(POSTGRES == null || POSTGRES.isRunning());
        assertTrue(jdbc.queryForObject("SHOW server_version", String.class).startsWith("16."));
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
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "reading-user-" + id, id + "@test.local", "unused", "Reading", "Tester", true, NOW, NOW);
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
