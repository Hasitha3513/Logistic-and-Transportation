package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleMeterResetRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingEventPublisher;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingTransaction;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.CoverageStatus;
import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VehicleMileageServiceTest {
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC);

    private InMemoryVehicleRepository vehicleRepository;
    private InMemoryVehicleReadingRepository readingRepository;
    private InMemoryVehicleMeterResetRepository resetRepository;
    private VehicleReadingService service;

    @BeforeEach
    void setUp() {
        vehicleRepository = new InMemoryVehicleRepository();
        readingRepository = new InMemoryVehicleReadingRepository();
        resetRepository = new InMemoryVehicleMeterResetRepository();

        vehicleRepository.save(new Vehicle(vehicleId, "WP-CAB-1201", "CHAS-01", "ENG-01",
                UUID.randomUUID(), UUID.randomUUID(), "Isuzu", "NPR", 2022, "OWNED",
                "AVAILABLE", 10000.0, 1000.0, 5000.0, true));

        VehicleReadingTransaction transaction = java.util.function.Supplier::get;
        VehicleReadingEventPublisher publisher = mock(VehicleReadingEventPublisher.class);

        service = new VehicleReadingService(vehicleRepository, readingRepository, resetRepository,
                transaction, publisher, clock);
    }

    @Test
    @DisplayName("Calculates simple period distance: 10,000 -> 10,500 = 500 km")
    void simpleDistance() {
        var t0 = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        var t1 = OffsetDateTime.parse("2026-08-10T10:00:00Z");

        saveReading(VehicleReadingType.ODOMETER, 10000.0, 0, VehicleReadingSourceType.BASELINE, null, t0);
        saveReading(VehicleReadingType.ODOMETER, 10500.0, 0, VehicleReadingSourceType.MANUAL, null, t1);

        var summary = service.mileageSummary(vehicleId,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-15T00:00:00Z"),
                true);

        assertThat(summary.openingOdometer()).isEqualByComparingTo("10000.000");
        assertThat(summary.closingOdometer()).isEqualByComparingTo("10500.000");
        assertThat(summary.distanceKm()).isEqualByComparingTo("500.000");
        assertThat(summary.coverageStatus()).isEqualTo(CoverageStatus.COMPLETE);
        assertThat(summary.readingCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Calculates engine hours used: 1,000 -> 1,050 = 50 hrs")
    void simpleEngineHours() {
        var t0 = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        var t1 = OffsetDateTime.parse("2026-08-10T10:00:00Z");

        saveReading(VehicleReadingType.ENGINE_HOURS, 1000.0, 0, VehicleReadingSourceType.BASELINE, null, t0);
        saveReading(VehicleReadingType.ENGINE_HOURS, 1050.0, 0, VehicleReadingSourceType.MANUAL, null, t1);

        var summary = service.mileageSummary(vehicleId,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-15T00:00:00Z"),
                false);

        assertThat(summary.openingEngineHours()).isEqualByComparingTo("1000.000");
        assertThat(summary.closingEngineHours()).isEqualByComparingTo("1050.000");
        assertThat(summary.engineHoursUsed()).isEqualByComparingTo("50.000");
        assertThat(summary.coverageStatus()).isEqualTo(CoverageStatus.COMPLETE);
    }

    @Test
    @DisplayName("Calculates mileage with backdated reading and chronological ordering")
    void backdatedReadingCalculation() {
        var t0 = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        var t1 = OffsetDateTime.parse("2026-08-05T10:00:00Z");
        var t2 = OffsetDateTime.parse("2026-08-10T10:00:00Z");

        saveReading(VehicleReadingType.ODOMETER, 10000.0, 0, VehicleReadingSourceType.BASELINE, null, t0);
        saveReading(VehicleReadingType.ODOMETER, 10500.0, 0, VehicleReadingSourceType.MANUAL, null, t2);
        // Backdated reading between t0 and t2
        saveReading(VehicleReadingType.ODOMETER, 10200.0, 0, VehicleReadingSourceType.FUEL_ISSUE, null, t1);

        var summary = service.mileageSummary(vehicleId,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-15T00:00:00Z"),
                true);

        assertThat(summary.openingOdometer()).isEqualByComparingTo("10000.000");
        assertThat(summary.closingOdometer()).isEqualByComparingTo("10500.000");
        assertThat(summary.distanceKm()).isEqualByComparingTo("500.000");
        assertThat(summary.readingCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Calculates mileage correctly after reading correction replaces corrupted value")
    void correctedReadingCalculation() {
        var t0 = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        var t1 = OffsetDateTime.parse("2026-08-10T10:00:00Z");

        saveReading(VehicleReadingType.ODOMETER, 10000.0, 0, VehicleReadingSourceType.BASELINE, null, t0);
        var badReading = saveReading(VehicleReadingType.ODOMETER, 10700.0, 0, VehicleReadingSourceType.MANUAL, null, t1);

        // Correct 10700 -> 10500
        var correctedReading = new VehicleReading(
                UUID.randomUUID(), vehicleId, VehicleReadingType.ODOMETER, BigDecimal.valueOf(10500.0).setScale(3),
                VehicleReadingUnit.KILOMETER, 0, VehicleReadingSourceType.MANUAL, null, t1,
                t1.plusMinutes(10), actorId, badReading.id(), "Typo correction from 10700 to 10500",
                "CORR-01", null, t1.plusMinutes(10));
        readingRepository.save(correctedReading);

        var summary = service.mileageSummary(vehicleId,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-15T00:00:00Z"),
                true);

        assertThat(summary.openingOdometer()).isEqualByComparingTo("10000.000");
        assertThat(summary.closingOdometer()).isEqualByComparingTo("10500.000");
        assertThat(summary.distanceKm()).isEqualByComparingTo("500.000");
        assertThat(summary.correctionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Multi-epoch physical meter reset calculates cross-epoch distance: 500 km + 800 km = 1,300 km")
    void multiEpochDistanceCalculation() {
        var t0 = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        var tPreReset = OffsetDateTime.parse("2026-08-05T12:00:00Z");
        var tReset = OffsetDateTime.parse("2026-08-05T14:00:00Z");
        var tPostReset = OffsetDateTime.parse("2026-08-10T10:00:00Z");

        // Epoch 0: 245,000 -> 245,500 (distance = 500)
        saveReading(VehicleReadingType.ODOMETER, 245000.0, 0, VehicleReadingSourceType.BASELINE, null, t0);
        saveReading(VehicleReadingType.ODOMETER, 245500.0, 0, VehicleReadingSourceType.MANUAL, null, tPreReset);

        // Meter reset to 0 at Epoch 1
        var resetReading = saveReading(VehicleReadingType.ODOMETER, 0.0, 1, VehicleReadingSourceType.METER_RESET, null, tReset);
        resetRepository.save(new VehicleMeterReset(UUID.randomUUID(), vehicleId, VehicleReadingType.ODOMETER,
                null, BigDecimal.valueOf(245500.0), resetReading.id(), BigDecimal.ZERO, tReset,
                "Instrument cluster replacement", actorId, null, null, tReset));

        // Epoch 1: 0 -> 800 (distance = 800)
        saveReading(VehicleReadingType.ODOMETER, 800.0, 1, VehicleReadingSourceType.MANUAL, null, tPostReset);

        var summary = service.mileageSummary(vehicleId,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-15T00:00:00Z"),
                true);

        assertThat(summary.openingOdometer()).isEqualByComparingTo("245000.000");
        assertThat(summary.closingOdometer()).isEqualByComparingTo("800.000");
        assertThat(summary.distanceKm()).isEqualByComparingTo("1300.000");
        assertThat(summary.meterResetCount()).isEqualTo(1);
        assertThat(summary.coverageStatus()).isEqualTo(CoverageStatus.COMPLETE);
    }

    @Test
    @DisplayName("Period coverage status: NO_DATA when vehicle has no readings")
    void noDataCoverage() {
        var summary = service.mileageSummary(vehicleId,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-15T00:00:00Z"),
                false);

        assertThat(summary.coverageStatus()).isEqualTo(CoverageStatus.NO_DATA);
        assertThat(summary.distanceKm()).isEqualByComparingTo("0.000");
        assertThat(summary.openingOdometer()).isNull();
        assertThat(summary.closingOdometer()).isNull();
    }

    @Test
    @DisplayName("Period coverage status: PARTIAL when no opening reading exists prior to start date")
    void partialCoverage() {
        var tMid = OffsetDateTime.parse("2026-08-05T12:00:00Z");
        var tEnd = OffsetDateTime.parse("2026-08-10T10:00:00Z");

        // Readings only exist starting mid-period
        saveReading(VehicleReadingType.ODOMETER, 10000.0, 0, VehicleReadingSourceType.MANUAL, null, tMid);
        saveReading(VehicleReadingType.ODOMETER, 10500.0, 0, VehicleReadingSourceType.MANUAL, null, tEnd);

        var summary = service.mileageSummary(vehicleId,
                OffsetDateTime.parse("2026-08-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-15T00:00:00Z"),
                false);

        assertThat(summary.coverageStatus()).isEqualTo(CoverageStatus.PARTIAL);
        assertThat(summary.coverageReason()).contains("No opening reading");
        assertThat(summary.distanceKm()).isEqualByComparingTo("500.000");
    }

    @Test
    @DisplayName("Authoritative Trip Distance: TRIP_START (10,000) -> FUEL_ISSUE (10,050) -> TRIP_END (10,100) = 100 km")
    void tripDistanceCalculation() {
        var tripId = UUID.randomUUID();
        var t0 = OffsetDateTime.parse("2026-08-01T08:00:00Z");
        var tFuel = OffsetDateTime.parse("2026-08-01T10:00:00Z");
        var tEnd = OffsetDateTime.parse("2026-08-01T14:00:00Z");

        saveReading(VehicleReadingType.ODOMETER, 10000.0, 0, VehicleReadingSourceType.TRIP_START, tripId, t0);
        saveReading(VehicleReadingType.ODOMETER, 10050.0, 0, VehicleReadingSourceType.FUEL_ISSUE, UUID.randomUUID(), tFuel);
        saveReading(VehicleReadingType.ODOMETER, 10100.0, 0, VehicleReadingSourceType.TRIP_END, tripId, tEnd);

        var tripDistance = service.tripDistance(tripId, vehicleId);

        assertThat(tripDistance.status()).isEqualTo(TripDistanceStatus.AVAILABLE);
        assertThat(tripDistance.startOdometer()).isEqualByComparingTo("10000.000");
        assertThat(tripDistance.endOdometer()).isEqualByComparingTo("10100.000");
        assertThat(tripDistance.distanceKm()).isEqualByComparingTo("100.000");
        assertThat(tripDistance.meterResetEncountered()).isFalse();
    }

    @Test
    @DisplayName("Trip Distance with corrected end reading: uses effective value (10,100) instead of superseded (10,500)")
    void tripDistanceWithCorrection() {
        var tripId = UUID.randomUUID();
        var t0 = OffsetDateTime.parse("2026-08-01T08:00:00Z");
        var tEnd = OffsetDateTime.parse("2026-08-01T14:00:00Z");

        saveReading(VehicleReadingType.ODOMETER, 10000.0, 0, VehicleReadingSourceType.TRIP_START, tripId, t0);
        var badEnd = saveReading(VehicleReadingType.ODOMETER, 10500.0, 0, VehicleReadingSourceType.TRIP_END, tripId, tEnd);

        // Correct end reading
        var correctedEnd = new VehicleReading(
                UUID.randomUUID(), vehicleId, VehicleReadingType.ODOMETER, BigDecimal.valueOf(10100.0).setScale(3),
                VehicleReadingUnit.KILOMETER, 0, VehicleReadingSourceType.TRIP_END, tripId, tEnd,
                tEnd.plusMinutes(5), actorId, badEnd.id(), "Corrected driver slip typo",
                "CORR-TRIP-END", null, tEnd.plusMinutes(5));
        readingRepository.save(correctedEnd);

        var tripDistance = service.tripDistance(tripId, vehicleId);

        assertThat(tripDistance.status()).isEqualTo(TripDistanceStatus.AVAILABLE);
        assertThat(tripDistance.startOdometer()).isEqualByComparingTo("10000.000");
        assertThat(tripDistance.endOdometer()).isEqualByComparingTo("10100.000");
        assertThat(tripDistance.distanceKm()).isEqualByComparingTo("100.000");
    }

    @Test
    @DisplayName("Trip in progress: returns PARTIAL status with start odometer and null distance")
    void tripInProgressDistance() {
        var tripId = UUID.randomUUID();
        var t0 = OffsetDateTime.parse("2026-08-01T08:00:00Z");

        saveReading(VehicleReadingType.ODOMETER, 10000.0, 0, VehicleReadingSourceType.TRIP_START, tripId, t0);

        var tripDistance = service.tripDistance(tripId, vehicleId);

        assertThat(tripDistance.status()).isEqualTo(TripDistanceStatus.PARTIAL);
        assertThat(tripDistance.startOdometer()).isEqualByComparingTo("10000.000");
        assertThat(tripDistance.endOdometer()).isNull();
        assertThat(tripDistance.distanceKm()).isNull();
        assertThat(tripDistance.notes()).contains("trip not yet completed");
    }

    private VehicleReading saveReading(VehicleReadingType type, double value, int epoch,
                                      VehicleReadingSourceType source, UUID sourceRef,
                                      OffsetDateTime recordedAt) {
        var reading = new VehicleReading(
                UUID.randomUUID(), vehicleId, type, BigDecimal.valueOf(value).setScale(3),
                type == VehicleReadingType.ODOMETER ? VehicleReadingUnit.KILOMETER : VehicleReadingUnit.HOUR,
                epoch, source, sourceRef, recordedAt, recordedAt, actorId, null, null,
                "IDEMP-" + UUID.randomUUID(), null, recordedAt);
        return readingRepository.save(reading);
    }

    private static class InMemoryVehicleRepository implements VehicleRepository {
        private final ConcurrentMap<UUID, Vehicle> map = new ConcurrentHashMap<>();

        @Override public Vehicle save(Vehicle vehicle) { map.put(vehicle.id(), vehicle); return vehicle; }
        @Override public Optional<Vehicle> findById(UUID id) { return Optional.ofNullable(map.get(id)); }
        @Override public Optional<Vehicle> findByIdForUpdate(UUID id) { return findById(id); }
        @Override public List<Vehicle> findAll() { return new ArrayList<>(map.values()); }
    }

    private static class InMemoryVehicleReadingRepository implements VehicleReadingRepository {
        private final List<VehicleReading> list = new ArrayList<>();

        @Override
        public synchronized VehicleReading save(VehicleReading reading) {
            list.removeIf(r -> r.id().equals(reading.id()));
            list.add(reading);
            return reading;
        }

        @Override
        public synchronized Optional<VehicleReading> findById(UUID readingId) {
            return list.stream().filter(r -> r.id().equals(readingId)).findFirst();
        }

        @Override
        public synchronized Optional<VehicleReading> findPreviousEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch, OffsetDateTime recordedAt) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type && r.meterEpoch() == meterEpoch)
                    .filter(r -> r.recordedAt().isBefore(recordedAt))
                    .filter(r -> !isSuperseded(r.id()))
                    .max(Comparator.comparing(VehicleReading::recordedAt));
        }

        @Override
        public synchronized Optional<VehicleReading> findNextEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch, OffsetDateTime recordedAt) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type && r.meterEpoch() == meterEpoch)
                    .filter(r -> r.recordedAt().isAfter(recordedAt))
                    .filter(r -> !isSuperseded(r.id()))
                    .min(Comparator.comparing(VehicleReading::recordedAt));
        }

        @Override
        public synchronized List<VehicleReading> findEffectiveAt(UUID vehicleId, VehicleReadingType type, int meterEpoch, OffsetDateTime recordedAt) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type && r.meterEpoch() == meterEpoch)
                    .filter(r -> r.recordedAt().isEqual(recordedAt))
                    .filter(r -> !isSuperseded(r.id()))
                    .toList();
        }

        @Override
        public synchronized Optional<VehicleReading> findLatestEffective(UUID vehicleId, VehicleReadingType type, int meterEpoch) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type && r.meterEpoch() == meterEpoch)
                    .filter(r -> !isSuperseded(r.id()))
                    .max(Comparator.comparing(VehicleReading::recordedAt));
        }

        @Override
        public synchronized int findCurrentMeterEpoch(UUID vehicleId, VehicleReadingType type) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type)
                    .mapToInt(VehicleReading::meterEpoch)
                    .max().orElse(0);
        }

        @Override
        public synchronized Optional<VehicleReading> findOriginalBySource(UUID vehicleId, VehicleReadingType type, VehicleReadingSourceType sourceType, UUID sourceReferenceId) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type && r.sourceType() == sourceType)
                    .filter(r -> sourceReferenceId == null || sourceReferenceId.equals(r.sourceReferenceId()))
                    .filter(r -> r.correctionOfReadingId() == null)
                    .findFirst();
        }

        @Override
        public synchronized Optional<VehicleReading> findByIdempotencyKey(String idempotencyKey) {
            return list.stream().filter(r -> idempotencyKey.equals(r.idempotencyKey())).findFirst();
        }

        @Override
        public synchronized Optional<VehicleReading> findCorrectionOf(UUID readingId) {
            return list.stream().filter(r -> readingId.equals(r.correctionOfReadingId())).findFirst();
        }

        @Override
        public synchronized boolean isSuperseded(UUID readingId) {
            return list.stream().anyMatch(r -> readingId.equals(r.correctionOfReadingId()));
        }

        @Override
        public synchronized List<VehicleReading> findEffectiveInPeriod(UUID vehicleId, VehicleReadingType type, OffsetDateTime from, OffsetDateTime to) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type)
                    .filter(r -> !r.recordedAt().isBefore(from) && !r.recordedAt().isAfter(to))
                    .filter(r -> !isSuperseded(r.id()))
                    .sorted(Comparator.comparing(VehicleReading::meterEpoch).thenComparing(VehicleReading::recordedAt))
                    .toList();
        }

        @Override
        public synchronized Optional<VehicleReading> findOpeningEffective(UUID vehicleId, VehicleReadingType type, OffsetDateTime from) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type)
                    .filter(r -> !r.recordedAt().isAfter(from))
                    .filter(r -> !isSuperseded(r.id()))
                    .max(Comparator.comparing(VehicleReading::recordedAt));
        }

        @Override
        public synchronized Optional<VehicleReading> findClosingEffective(UUID vehicleId, VehicleReadingType type, OffsetDateTime to) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == type)
                    .filter(r -> !r.recordedAt().isAfter(to))
                    .filter(r -> !isSuperseded(r.id()))
                    .max(Comparator.comparing(VehicleReading::recordedAt));
        }

        @Override
        public synchronized Optional<VehicleReading> findEffectiveBySource(UUID vehicleId, VehicleReadingType type, VehicleReadingSourceType sourceType, UUID sourceReferenceId) {
            var original = findOriginalBySource(vehicleId, type, sourceType, sourceReferenceId);
            if (original.isEmpty()) return Optional.empty();
            var current = original.get();
            while (true) {
                var corr = findCorrectionOf(current.id());
                if (corr.isEmpty()) return Optional.of(current);
                current = corr.get();
            }
        }

        @Override
        public synchronized int countCorrectionsInPeriod(UUID vehicleId, OffsetDateTime from, OffsetDateTime to) {
            return (int) list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId))
                    .filter(r -> r.correctionOfReadingId() != null)
                    .filter(r -> !r.recordedAt().isBefore(from) && !r.recordedAt().isAfter(to))
                    .count();
        }

        @Override
        public synchronized List<VehicleReading> findAllInPeriod(UUID vehicleId, OffsetDateTime from, OffsetDateTime to) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId))
                    .filter(r -> !r.recordedAt().isBefore(from) && !r.recordedAt().isAfter(to))
                    .sorted(Comparator.comparing(VehicleReading::recordedAt))
                    .toList();
        }

        @Override
        public synchronized VehicleReadingUseCase.PageResult<VehicleReading> search(VehicleReadingUseCase.SearchQuery query) {
            return new VehicleReadingUseCase.PageResult<>(list, 0, 20, list.size(), 1);
        }
    }

    private static class InMemoryVehicleMeterResetRepository implements VehicleMeterResetRepository {
        private final List<VehicleMeterReset> list = new ArrayList<>();

        @Override
        public synchronized VehicleMeterReset save(VehicleMeterReset reset) {
            list.add(reset);
            return reset;
        }

        @Override
        public synchronized List<VehicleMeterReset> findByVehicleId(UUID vehicleId) {
            return list.stream().filter(r -> r.vehicleId().equals(vehicleId)).toList();
        }

        @Override
        public synchronized Optional<VehicleMeterReset> findById(UUID resetId) {
            return list.stream().filter(r -> r.id().equals(resetId)).findFirst();
        }

        @Override
        public synchronized Optional<VehicleMeterReset> findLatestByVehicleIdAndReadingType(UUID vehicleId, com.transportlogistics.app.fleet.domain.model.VehicleReadingType readingType) {
            return list.stream()
                    .filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == readingType)
                    .max(Comparator.comparing(VehicleMeterReset::effectiveAt));
        }
    }
}
