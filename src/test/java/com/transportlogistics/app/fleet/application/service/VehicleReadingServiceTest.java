package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.CoverageStatus;
import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleMeterResetRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingEventPublisher;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingTransaction;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.vehiclemaster.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleMeterReset;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class VehicleReadingServiceTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-16T12:00:00Z");

    private InMemoryVehicleRepository vehicles;
    private InMemoryVehicleReadingRepository readings;
    private InMemoryVehicleMeterResetRepository meterResets;
    private List<Object> publishedEvents;
    private VehicleReadingService service;
    private UUID vehicleId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        vehicles = new InMemoryVehicleRepository();
        readings = new InMemoryVehicleReadingRepository();
        meterResets = new InMemoryVehicleMeterResetRepository();
        publishedEvents = new ArrayList<>();
        vehicleId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        vehicles.save(new Vehicle(vehicleId, "WP-CAD-1234", "1HGCR2F83HA000000", "K24W-1000000", UUID.randomUUID(),
                UUID.randomUUID(), "Toyota", "Hilux", 2024, "OWNED", "AVAILABLE", 10000.0, null, 1000.0, true));

        service = new VehicleReadingService(
                vehicles,
                readings,
                meterResets,
                new DirectTransaction(),
                new RecordingEventPublisher(publishedEvents),
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
        );
    }

    @Test
    void recordsManualOdometerReadingSuccessfully() {
        var command = new VehicleReadingUseCase.RecordCommand(
                vehicleId,
                VehicleReadingType.ODOMETER,
                new BigDecimal("10500.000"),
                VehicleReadingSourceType.MANUAL,
                null,
                OffsetDateTime.parse("2026-08-16T11:00:00Z"),
                actorId,
                "key-1",
                "Baseline test"
        );

        var result = service.record(command);
        assertNotNull(result.id());
        assertEquals(new BigDecimal("10500.000"), result.value());
        assertEquals(0, result.meterEpoch());
        assertEquals(VehicleReadingSourceType.MANUAL, result.sourceType());
        assertNull(result.correctionOfReadingId());

        var vehicle = vehicles.findById(vehicleId).orElseThrow();
        assertEquals(10500.0, vehicle.currentOdometerKm());
    }

    @Test
    void rejectsDecreasingOdometerReadingInSameEpoch() {
        service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("10500.000"),
                VehicleReadingSourceType.MANUAL, null, OffsetDateTime.parse("2026-08-16T10:00:00Z"),
                actorId, "key-1", "Initial"
        ));

        var ex = assertThrows(ConflictException.class, () -> service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("10400.000"),
                VehicleReadingSourceType.MANUAL, null, OffsetDateTime.parse("2026-08-16T11:00:00Z"),
                actorId, "key-2", "Decreasing"
        )));
        assertEquals("VEHICLE_READING_DECREASE", ex.code());
    }

    @Test
    void correctsReadingPreservingOriginalAuditTrail() {
        var original = service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("10500.000"),
                VehicleReadingSourceType.MANUAL, null, OffsetDateTime.parse("2026-08-16T10:00:00Z"),
                actorId, "key-1", "Typo in original"
        ));

        var correctCommand = new VehicleReadingUseCase.CorrectCommand(
                vehicleId,
                original.id(),
                new BigDecimal("10600.000"),
                "Corrected typo from driver log",
                original.recordedAt(),
                actorId
        );

        var correction = service.correct(correctCommand);
        assertNotNull(correction.id());
        assertNotEquals(original.id(), correction.id());
        assertEquals(original.id(), correction.correctionOfReadingId());
        assertEquals("Corrected typo from driver log", correction.correctionReason());
        assertEquals(new BigDecimal("10600.000"), correction.value());

        var persistedOriginal = service.get(original.id());
        assertEquals(new BigDecimal("10500.000"), persistedOriginal.value());

        // Attempting second correction on same original reading must be rejected
        var duplicateCorrectionEx = assertThrows(ConflictException.class, () -> service.correct(correctCommand));
        assertEquals("DUPLICATE_VEHICLE_READING_CORRECTION", duplicateCorrectionEx.code());
    }

    @Test
    void resetsMeterAdvancingEpochAndAcceptingLowerValue() {
        service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("250000.000"),
                VehicleReadingSourceType.MANUAL, null, OffsetDateTime.parse("2026-08-16T08:00:00Z"),
                actorId, "key-1", "Old meter end"
        ));

        var resetTime = OffsetDateTime.parse("2026-08-16T09:00:00Z");
        var reset = service.resetMeter(new VehicleReadingUseCase.ResetMeterCommand(
                vehicleId,
                VehicleReadingType.ODOMETER,
                new BigDecimal("0.000"),
                resetTime,
                "Physical odometer replaced under warranty",
                actorId
        ));

        assertNotNull(reset.id());
        assertEquals(0, reset.fromEpoch());
        assertEquals(1, reset.toEpoch());
        assertEquals(new BigDecimal("250000.000"), reset.lastReadingValue());
        assertEquals(new BigDecimal("0.000"), reset.newMeterValue());

        // In new epoch 1, reading 50 km (which is lower than 250,000) is accepted!
        var nextReading = service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("50.000"),
                VehicleReadingSourceType.MANUAL, null, resetTime.plusHours(1),
                actorId, "key-new-1", "New meter trip"
        ));
        assertEquals(1, nextReading.meterEpoch());
        assertEquals(new BigDecimal("50.000"), nextReading.value());
    }

    @Test
    void calculatesMileageAccuratelyAcrossMeterResets() {
        var t1 = OffsetDateTime.parse("2026-08-10T08:00:00Z");
        var t2 = OffsetDateTime.parse("2026-08-11T08:00:00Z");
        var tReset = OffsetDateTime.parse("2026-08-12T08:00:00Z");
        var t3 = OffsetDateTime.parse("2026-08-13T08:00:00Z");

        // Epoch 0: 10000 -> 10500 (distance = 500 km)
        service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("10000.000"),
                VehicleReadingSourceType.BASELINE, null, t1, actorId, "k1", "Start"
        ));
        service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("10500.000"),
                VehicleReadingSourceType.MANUAL, null, t2, actorId, "k2", "Mid"
        ));

        // Reset to 0 at tReset
        service.resetMeter(new VehicleReadingUseCase.ResetMeterCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("0.000"),
                tReset, "Meter replacement", actorId
        ));

        // Epoch 1: 0 -> 150 km (distance = 150 km)
        service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("150.000"),
                VehicleReadingSourceType.MANUAL, null, t3, actorId, "k3", "New meter"
        ));

        var mileage = service.getMileage(vehicleId, t1.minusDays(1), t3.plusDays(1));
        assertEquals(new BigDecimal("10000.000"), mileage.openingOdometer());
        assertEquals(new BigDecimal("150.000"), mileage.closingOdometer());
        assertEquals(new BigDecimal("650.000"), mileage.distanceTravelledKm()); // 500 + 150 = 650 km
        assertEquals(1, mileage.meterResetCount());
        assertEquals(CoverageStatus.COMPLETE, mileage.coverageStatus());
    }

    @Test
    void calculatesTripDistanceCorrectly() {
        var tripId = UUID.randomUUID();
        var tStart = OffsetDateTime.parse("2026-08-16T08:00:00Z");
        var tEnd = OffsetDateTime.parse("2026-08-16T12:00:00Z");

        service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("10010.000"),
                VehicleReadingSourceType.TRIP_START, tripId, tStart, actorId, null, "Trip start"
        ));
        service.record(new VehicleReadingUseCase.RecordCommand(
                vehicleId, VehicleReadingType.ODOMETER, new BigDecimal("10100.000"),
                VehicleReadingSourceType.TRIP_END, tripId, tEnd, actorId, null, "Trip end"
        ));

        var tripDist = service.calculateTripDistance(tripId);
        assertEquals(TripDistanceStatus.CALCULATED, tripDist.status());
        assertEquals(vehicleId, tripDist.vehicleId());
        assertEquals(new BigDecimal("10010.000"), tripDist.startOdometerKm());
        assertEquals(new BigDecimal("10100.000"), tripDist.endOdometerKm());
        assertEquals(new BigDecimal("90.000"), tripDist.distanceTravelledKm());
    }

    private static class DirectTransaction implements VehicleReadingTransaction {
        @Override public <T> T execute(Supplier<T> operation) { return operation.get(); }
    }

    private static class RecordingEventPublisher implements VehicleReadingEventPublisher {
        private final List<Object> events;
        RecordingEventPublisher(List<Object> events) { this.events = events; }
        @Override public void publishAfterCommit(com.transportlogistics.app.fleet.VehicleReadingRecorded event) { events.add(event); }
        @Override public void publishAfterCommit(com.transportlogistics.app.fleet.VehicleReadingCorrected event) { events.add(event); }
        @Override public void publishAfterCommit(com.transportlogistics.app.fleet.VehicleMeterResetRecorded event) { events.add(event); }
    }

    private static class InMemoryVehicleRepository implements VehicleRepository {
        private final Map<UUID, Vehicle> store = new HashMap<>();
        @Override public Vehicle save(Vehicle vehicle) { store.put(vehicle.id(), vehicle); return vehicle; }
        @Override public Optional<Vehicle> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Vehicle> findByIdForUpdate(UUID id) { return findById(id); }
        @Override public List<Vehicle> findAll() { return List.copyOf(store.values()); }
        @Override public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
            return store.values().stream().filter(v -> v.registrationNumber().equalsIgnoreCase(registrationNumber)).findFirst();
        }
        @Override public Optional<Vehicle> findByChassisNumber(String chassisNumber) {
            return store.values().stream().filter(v -> v.chassisNumber() != null && v.chassisNumber().equalsIgnoreCase(chassisNumber)).findFirst();
        }
        @Override public Optional<Vehicle> findByEngineNumber(String engineNumber) {
            return store.values().stream().filter(v -> v.engineNumber() != null && v.engineNumber().equalsIgnoreCase(engineNumber)).findFirst();
        }
        @Override public boolean existsByRegistrationNumberAndIdNot(String registrationNumber, UUID id) {
            return store.values().stream().anyMatch(v -> !v.id().equals(id) && v.registrationNumber().equalsIgnoreCase(registrationNumber));
        }
        @Override public boolean existsByChassisNumberAndIdNot(String chassisNumber, UUID id) {
            return store.values().stream().anyMatch(v -> !v.id().equals(id) && v.chassisNumber() != null && v.chassisNumber().equalsIgnoreCase(chassisNumber));
        }
        @Override public boolean existsByEngineNumberAndIdNot(String engineNumber, UUID id) {
            return store.values().stream().anyMatch(v -> !v.id().equals(id) && v.engineNumber() != null && v.engineNumber().equalsIgnoreCase(engineNumber));
        }
    }

    private static class InMemoryVehicleMeterResetRepository implements VehicleMeterResetRepository {
        private final List<VehicleMeterReset> resets = new ArrayList<>();
        @Override public VehicleMeterReset save(VehicleMeterReset reset) { resets.add(reset); return reset; }
        @Override public List<VehicleMeterReset> findByVehicleId(UUID vehicleId) {
            return resets.stream().filter(r -> r.vehicleId().equals(vehicleId)).sorted(Comparator.comparing(VehicleMeterReset::effectiveAt).reversed()).toList();
        }
        @Override public List<VehicleMeterReset> findByVehicleIdAndType(UUID vehicleId, VehicleReadingType readingType) {
            return resets.stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == readingType).sorted(Comparator.comparing(VehicleMeterReset::effectiveAt).reversed()).toList();
        }
        @Override public Optional<VehicleMeterReset> findLatestByVehicleIdAndType(UUID vehicleId, VehicleReadingType readingType) {
            return findByVehicleIdAndType(vehicleId, readingType).stream().findFirst();
        }
    }

    private static class InMemoryVehicleReadingRepository implements VehicleReadingRepository {
        private final Map<UUID, VehicleReading> store = new HashMap<>();

        @Override public VehicleReading save(VehicleReading reading) { store.put(reading.id(), reading); return reading; }
        @Override public Optional<VehicleReading> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<VehicleReading> findByIdempotencyKey(String idempotencyKey) {
            if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
            return store.values().stream().filter(r -> idempotencyKey.equals(r.idempotencyKey())).findFirst();
        }
        @Override public Optional<VehicleReading> findOriginalBySource(UUID vehicleId, VehicleReadingType readingType, VehicleReadingSourceType sourceType, UUID sourceReferenceId) {
            return store.values().stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == readingType && r.sourceType() == sourceType && Objects.equals(r.sourceReferenceId(), sourceReferenceId) && r.correctionOfReadingId() == null).findFirst();
        }
        @Override public Optional<VehicleReading> findLatestEffective(UUID vehicleId, VehicleReadingType readingType, int meterEpoch) {
            return store.values().stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == readingType && r.meterEpoch() == meterEpoch && !isSuperseded(r)).max(Comparator.comparing(VehicleReading::recordedAt));
        }
        @Override public Optional<VehicleReading> findPreviousEffective(UUID vehicleId, VehicleReadingType readingType, int meterEpoch, OffsetDateTime recordedAt) {
            return store.values().stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == readingType && r.meterEpoch() == meterEpoch && r.recordedAt().isBefore(recordedAt) && !isSuperseded(r)).max(Comparator.comparing(VehicleReading::recordedAt));
        }
        @Override public Optional<VehicleReading> findNextEffective(UUID vehicleId, VehicleReadingType readingType, int meterEpoch, OffsetDateTime recordedAt) {
            return store.values().stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == readingType && r.meterEpoch() == meterEpoch && r.recordedAt().isAfter(recordedAt) && !isSuperseded(r)).min(Comparator.comparing(VehicleReading::recordedAt));
        }
        @Override public List<VehicleReading> findEffectiveAt(UUID vehicleId, VehicleReadingType readingType, int meterEpoch, OffsetDateTime recordedAt) {
            return store.values().stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == readingType && r.meterEpoch() == meterEpoch && r.recordedAt().isEqual(recordedAt) && !isSuperseded(r)).toList();
        }
        @Override public Optional<VehicleReading> findCorrection(UUID originalReadingId) {
            return store.values().stream().filter(r -> originalReadingId.equals(r.correctionOfReadingId())).findFirst();
        }
        @Override public int findCurrentMeterEpoch(UUID vehicleId, VehicleReadingType readingType) {
            return store.values().stream().filter(r -> r.vehicleId().equals(vehicleId) && r.readingType() == readingType).mapToInt(VehicleReading::meterEpoch).max().orElse(0);
        }
        @Override public VehicleReadingUseCase.PageResult<VehicleReading> search(VehicleReadingUseCase.SearchQuery query) {
            var list = store.values().stream()
                    .filter(r -> query.vehicleId() == null || r.vehicleId().equals(query.vehicleId()))
                    .filter(r -> query.readingType() == null || r.readingType() == query.readingType())
                    .filter(r -> query.sourceType() == null || r.sourceType() == query.sourceType())
                    .sorted(Comparator.comparing(VehicleReading::recordedAt).reversed())
                    .toList();
            return new VehicleReadingUseCase.PageResult<>(list, query.page(), query.limit(), list.size(), 1);
        }
        private boolean isSuperseded(VehicleReading r) {
            return r.correctionOfReadingId() == null && store.values().stream().anyMatch(other -> r.id().equals(other.correctionOfReadingId()));
        }
    }
}
