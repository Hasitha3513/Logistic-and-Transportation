package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.VehicleReadingRecorded;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingEventPublisher;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingTransaction;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VehicleReadingServiceTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-16T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();

    private FakeVehicles vehicles;
    private FakeReadings readings;
    private List<VehicleReadingRecorded> events;
    private VehicleReadingService service;
    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        vehicles = new FakeVehicles();
        readings = new FakeReadings();
        events = new ArrayList<>();
        vehicleId = UUID.randomUUID();
        vehicles.save(vehicle(vehicleId, true));
        service = new VehicleReadingService(vehicles, readings, new DirectTransaction(), events::add,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
    }

    @Test
    void acceptsFirstAndIncreasingOdometerAndSynchronizesSnapshot() {
        var first = manual(VehicleReadingType.ODOMETER, "10000", NOW.minusDays(2), "first");
        var second = manual(VehicleReadingType.ODOMETER, "10200", NOW.minusDays(1), "second");

        assertEquals(new BigDecimal("10000.000"), first.value());
        assertEquals(new BigDecimal("10200.000"), second.value());
        assertEquals(10200d, vehicles.findById(vehicleId).orElseThrow().currentOdometerKm());
        assertEquals(2, events.size());
    }

    @Test
    void rejectsDecreasingLatestOdometer() {
        manual(VehicleReadingType.ODOMETER, "10000", NOW.minusDays(2), "first");

        var error = assertThrows(ConflictException.class,
                () -> manual(VehicleReadingType.ODOMETER, "9999", NOW.minusDays(1), "lower"));

        assertEquals("VEHICLE_READING_DECREASE", error.code());
        assertEquals(1, readings.values.size());
    }

    @Test
    void acceptsIncreasingEngineHoursAndRejectsDecrease() {
        manual(VehicleReadingType.ENGINE_HOURS, "150.25", NOW.minusDays(2), "engine-1");
        manual(VehicleReadingType.ENGINE_HOURS, "151.00", NOW.minusDays(1), "engine-2");

        assertEquals(151d, vehicles.findById(vehicleId).orElseThrow().engineHours());
        assertEquals("VEHICLE_READING_DECREASE", assertThrows(ConflictException.class,
                () -> manual(VehicleReadingType.ENGINE_HOURS, "149", NOW, "engine-lower")).code());
    }

    @Test
    void rejectsNegativeReading() {
        var error = assertThrows(BusinessRuleException.class,
                () -> manual(VehicleReadingType.ODOMETER, "-1", NOW, "negative"));
        assertEquals("INVALID_VEHICLE_READING", error.code());
    }

    @Test
    void acceptsBackdatedReadingWithinBothNeighbors() {
        manual(VehicleReadingType.ODOMETER, "10000", NOW.minusDays(6), "day-10");
        manual(VehicleReadingType.ODOMETER, "10200", NOW.minusDays(4), "day-12");

        var backdated = manual(VehicleReadingType.ODOMETER, "10100", NOW.minusDays(5), "day-11");

        assertEquals(new BigDecimal("10100.000"), backdated.value());
        assertEquals(10200d, vehicles.findById(vehicleId).orElseThrow().currentOdometerKm());
    }

    @Test
    void rejectsBackdatedReadingBelowPrevious() {
        manual(VehicleReadingType.ODOMETER, "10000", NOW.minusDays(6), "day-10");
        manual(VehicleReadingType.ODOMETER, "10200", NOW.minusDays(4), "day-12");

        var error = assertThrows(ConflictException.class,
                () -> manual(VehicleReadingType.ODOMETER, "9900", NOW.minusDays(5), "too-low"));
        assertEquals("VEHICLE_READING_CHRONOLOGY_CONFLICT", error.code());
    }

    @Test
    void rejectsBackdatedReadingAboveNext() {
        manual(VehicleReadingType.ODOMETER, "10000", NOW.minusDays(6), "day-10");
        manual(VehicleReadingType.ODOMETER, "10200", NOW.minusDays(4), "day-12");

        var error = assertThrows(ConflictException.class,
                () -> manual(VehicleReadingType.ODOMETER, "10500", NOW.minusDays(5), "too-high"));
        assertEquals("VEHICLE_READING_CHRONOLOGY_CONFLICT", error.code());
    }

    @Test
    void sameTimestampAllowsEqualOperationalEvidenceAndRejectsDifferentValue() {
        var at = NOW.minusHours(2);
        system(VehicleReadingSourceType.TRIP_START, UUID.randomUUID(), "10000", at);
        system(VehicleReadingSourceType.FUEL_ISSUE, UUID.randomUUID(), "10000", at);

        var error = assertThrows(ConflictException.class,
                () -> system(VehicleReadingSourceType.TRIP_END, UUID.randomUUID(), "10001", at));
        assertEquals("VEHICLE_READING_CHRONOLOGY_CONFLICT", error.code());
        assertEquals(2, readings.values.size());
    }

    @Test
    void rejectsMissingAndInactiveVehicles() {
        var missing = UUID.randomUUID();
        var error = assertThrows(NotFoundException.class, () -> service.record(command(missing,
                VehicleReadingType.ODOMETER, "1", NOW, VehicleReadingSourceType.MANUAL, null, "missing")));
        assertEquals("VEHICLE_NOT_FOUND", error.code());

        var inactive = UUID.randomUUID();
        vehicles.save(vehicle(inactive, false));
        assertEquals("INVALID_VEHICLE_READING", assertThrows(BusinessRuleException.class,
                () -> service.record(command(inactive, VehicleReadingType.ODOMETER, "1", NOW,
                        VehicleReadingSourceType.MANUAL, null, "inactive"))).code());
    }

    @Test
    void identicalSourceReplayIsIdempotentAndChangedReplayIsRejected() {
        var reference = UUID.randomUUID();
        var first = system(VehicleReadingSourceType.TRIP_START, reference, "10000", NOW.minusHours(1));
        var replay = system(VehicleReadingSourceType.TRIP_START, reference, "10000", NOW.minusHours(1));

        assertEquals(first.id(), replay.id());
        assertEquals(1, readings.values.size());
        assertEquals("DUPLICATE_VEHICLE_READING", assertThrows(ConflictException.class,
                () -> system(VehicleReadingSourceType.TRIP_START, reference, "10001", NOW.minusHours(1))).code());
    }

    @Test
    void returnsLatestByRecordedTimeForBothTypes() {
        manual(VehicleReadingType.ODOMETER, "10000", NOW.minusDays(3), "odo-1");
        manual(VehicleReadingType.ODOMETER, "10200", NOW.minusDays(1), "odo-2");
        manual(VehicleReadingType.ODOMETER, "10100", NOW.minusDays(2), "odo-backdated");
        manual(VehicleReadingType.ENGINE_HOURS, "250", NOW.minusHours(1), "engine");

        var latest = service.latest(vehicleId);

        assertEquals(new BigDecimal("10200.000"), latest.odometer().orElseThrow().value());
        assertEquals(new BigDecimal("250.000"), latest.engineHours().orElseThrow().value());
    }

    @Test
    void pagesAndFiltersWithoutLoadingCompleteHistory() {
        manual(VehicleReadingType.ODOMETER, "10000", NOW.minusDays(3), "page-1");
        manual(VehicleReadingType.ODOMETER, "10100", NOW.minusDays(2), "page-2");
        manual(VehicleReadingType.ENGINE_HOURS, "10", NOW.minusDays(1), "page-3");

        var page = service.list(new VehicleReadingUseCase.SearchQuery(vehicleId, VehicleReadingType.ODOMETER,
                VehicleReadingSourceType.MANUAL, null, null, 0, 1));

        assertEquals(1, page.content().size());
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(VehicleReadingType.ODOMETER, page.content().getFirst().readingType());
    }

    private VehicleReading manual(VehicleReadingType type, String value, OffsetDateTime at, String key) {
        return service.record(command(vehicleId, type, value, at, VehicleReadingSourceType.MANUAL, null, key));
    }

    private VehicleReading system(VehicleReadingSourceType source, UUID reference, String value, OffsetDateTime at) {
        return service.record(command(vehicleId, VehicleReadingType.ODOMETER, value, at, source, reference, null));
    }

    private VehicleReadingUseCase.RecordCommand command(UUID targetVehicle, VehicleReadingType type, String value,
                                                        OffsetDateTime at, VehicleReadingSourceType source,
                                                        UUID reference, String key) {
        return new VehicleReadingUseCase.RecordCommand(targetVehicle, type, new BigDecimal(value), source, reference,
                at, ACTOR, key, "test reading");
    }

    private Vehicle vehicle(UUID id, boolean active) {
        return new Vehicle(id, "REG-" + id, null, null, UUID.randomUUID(), UUID.randomUUID(), "Maker", "Model",
                2026, "OWNED", "AVAILABLE", null, null, 1000d, active);
    }

    private static final class DirectTransaction implements VehicleReadingTransaction {
        @Override public <T> T execute(Supplier<T> operation) { return operation.get(); }
    }

    private static final class FakeVehicles implements VehicleRepository {
        private final HashMap<UUID, Vehicle> values = new HashMap<>();
        @Override public Vehicle save(Vehicle value) { values.put(value.id(), value); return value; }
        @Override public Optional<Vehicle> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        @Override public Optional<Vehicle> findByIdForUpdate(UUID id) { return findById(id); }
        @Override public List<Vehicle> findAll() { return List.copyOf(values.values()); }
    }

    private static final class FakeReadings implements VehicleReadingRepository {
        private final List<VehicleReading> values = new ArrayList<>();
        @Override public VehicleReading save(VehicleReading reading) { values.add(reading); return reading; }
        @Override public Optional<VehicleReading> findById(UUID id) {
            return values.stream().filter(value -> value.id().equals(id)).findFirst();
        }
        @Override public Optional<VehicleReading> findPreviousEffective(UUID vehicleId, VehicleReadingType type,
                                                                        int epoch, OffsetDateTime at) {
            return partition(vehicleId, type, epoch).stream().filter(r -> r.recordedAt().isBefore(at))
                    .max(Comparator.comparing(VehicleReading::recordedAt));
        }
        @Override public Optional<VehicleReading> findNextEffective(UUID vehicleId, VehicleReadingType type,
                                                                    int epoch, OffsetDateTime at) {
            return partition(vehicleId, type, epoch).stream().filter(r -> r.recordedAt().isAfter(at))
                    .min(Comparator.comparing(VehicleReading::recordedAt));
        }
        @Override public List<VehicleReading> findEffectiveAt(UUID vehicleId, VehicleReadingType type, int epoch,
                                                               OffsetDateTime at) {
            return partition(vehicleId, type, epoch).stream().filter(r -> r.recordedAt().isEqual(at)).toList();
        }
        @Override public Optional<VehicleReading> findLatestEffective(UUID vehicleId, VehicleReadingType type,
                                                                      int epoch) {
            return partition(vehicleId, type, epoch).stream().max(Comparator.comparing(VehicleReading::recordedAt)
                    .thenComparing(VehicleReading::receivedAt));
        }
        @Override public int findCurrentMeterEpoch(UUID vehicleId, VehicleReadingType type) {
            return values.stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type)
                    .mapToInt(VehicleReading::meterEpoch).max().orElse(0);
        }
        @Override public Optional<VehicleReading> findOriginalBySource(UUID vehicleId, VehicleReadingType type,
                                                                       VehicleReadingSourceType source,
                                                                       UUID reference) {
            return values.stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type
                    && r.sourceType() == source && java.util.Objects.equals(r.sourceReferenceId(), reference)
                    && r.correctionOfReadingId() == null).findFirst();
        }
        @Override public Optional<VehicleReading> findByIdempotencyKey(String key) {
            return values.stream().filter(r -> key.equals(r.idempotencyKey())).findFirst();
        }
        @Override public VehicleReadingUseCase.PageResult<VehicleReading> search(VehicleReadingUseCase.SearchQuery q) {
            var filtered = values.stream().filter(r -> r.vehicleId().equals(q.vehicleId()))
                    .filter(r -> q.readingType() == null || r.readingType() == q.readingType())
                    .filter(r -> q.sourceType() == null || r.sourceType() == q.sourceType())
                    .filter(r -> q.from() == null || !r.recordedAt().isBefore(q.from()))
                    .filter(r -> q.to() == null || !r.recordedAt().isAfter(q.to()))
                    .sorted(Comparator.comparing(VehicleReading::recordedAt).reversed()).toList();
            var from = Math.min(q.page() * q.limit(), filtered.size());
            var to = Math.min(from + q.limit(), filtered.size());
            var pages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / q.limit());
            return new VehicleReadingUseCase.PageResult<>(filtered.subList(from, to), q.page(), q.limit(),
                    filtered.size(), pages);
        }
        private List<VehicleReading> partition(UUID vehicleId, VehicleReadingType type, int epoch) {
            return values.stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type
                    && r.meterEpoch() == epoch).toList();
        }
    }
}
