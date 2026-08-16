package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.VehicleReadingRecorded;
import com.transportlogistics.app.fleet.VehicleReadingRecorder;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingEventPublisher;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingTransaction;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.Vehicle;
import com.transportlogistics.app.fleet.domain.model.VehicleReading;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingSourceType;
import com.transportlogistics.app.fleet.domain.model.VehicleReadingType;
import com.transportlogistics.app.fleet.domain.service.VehicleReadingChronologyPolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

public final class VehicleReadingService implements VehicleReadingUseCase, VehicleReadingRecorder {
    private static final int MAX_PAGE_SIZE = 100;

    private final VehicleRepository vehicles;
    private final VehicleReadingRepository readings;
    private final VehicleReadingTransaction transactions;
    private final VehicleReadingEventPublisher events;
    private final Clock clock;
    private final VehicleReadingChronologyPolicy chronology = new VehicleReadingChronologyPolicy();

    public VehicleReadingService(VehicleRepository vehicles, VehicleReadingRepository readings,
                                 VehicleReadingTransaction transactions, VehicleReadingEventPublisher events,
                                 Clock clock) {
        this.vehicles = vehicles;
        this.readings = readings;
        this.transactions = transactions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public VehicleReading record(RecordCommand command) {
        if (command == null) invalid("Reading command is required");
        return transactions.execute(() -> recordLocked(command));
    }

    @Override
    public Result record(Command command) {
        if (command == null) invalid("Reading command is required");
        var reading = record(new RecordCommand(command.vehicleId(), domainType(command.readingType()),
                command.value(), domainSource(command.sourceType()), command.sourceReferenceId(),
                command.recordedAt(), command.actorId(), null, null));
        return new Result(reading.id(), reading.vehicleId(), publicType(reading.readingType()), reading.value(),
                reading.unit().name(), publicSource(reading.sourceType()), reading.sourceReferenceId(),
                reading.recordedAt(), reading.receivedAt());
    }

    @Override
    public VehicleReading get(UUID readingId) {
        return readings.findById(readingId).orElseThrow(() -> new NotFoundException(
                "VEHICLE_READING_NOT_FOUND", "Vehicle reading not found: " + readingId));
    }

    @Override
    public PageResult<VehicleReading> list(SearchQuery query) {
        if (query == null || query.vehicleId() == null) invalid("Vehicle id is required");
        requireVehicle(query.vehicleId());
        if (query.page() < 0) invalid("Page cannot be negative");
        if (query.limit() < 1 || query.limit() > MAX_PAGE_SIZE) {
            invalid("Limit must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (query.from() != null && query.to() != null && query.to().isBefore(query.from())) {
            invalid("Reading date range is invalid");
        }
        return readings.search(query);
    }

    @Override
    public LatestReadings latest(UUID vehicleId) {
        requireVehicle(vehicleId);
        var odometerEpoch = readings.findCurrentMeterEpoch(vehicleId, VehicleReadingType.ODOMETER);
        var engineEpoch = readings.findCurrentMeterEpoch(vehicleId, VehicleReadingType.ENGINE_HOURS);
        return new LatestReadings(vehicleId,
                readings.findLatestEffective(vehicleId, VehicleReadingType.ODOMETER, odometerEpoch),
                readings.findLatestEffective(vehicleId, VehicleReadingType.ENGINE_HOURS, engineEpoch));
    }

    private VehicleReading recordLocked(RecordCommand command) {
        validateCommand(command);
        var vehicle = vehicles.findByIdForUpdate(command.vehicleId()).orElseThrow(() ->
                new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: " + command.vehicleId()));
        if (!vehicle.active() && command.sourceType() != VehicleReadingSourceType.BASELINE) {
            invalid("Inactive vehicles cannot receive ordinary readings");
        }

        var normalized = normalize(command.value());
        var idempotencyKey = idempotencyKey(command);
        var replay = readings.findByIdempotencyKey(idempotencyKey)
                .or(() -> sourceReplay(command));
        if (replay.isPresent()) {
            return requireEquivalentReplay(replay.orElseThrow(), command, normalized);
        }

        var epoch = readings.findCurrentMeterEpoch(command.vehicleId(), command.readingType());
        var now = now();
        var candidate = new VehicleReading(UUID.randomUUID(), command.vehicleId(), command.readingType(), normalized,
                command.readingType().unit(), epoch, command.sourceType(), command.sourceReferenceId(),
                command.recordedAt(), now, command.actorId(), null, null, idempotencyKey, command.notes(), now);
        var previous = readings.findPreviousEffective(candidate.vehicleId(), candidate.readingType(), epoch,
                candidate.recordedAt()).orElse(null);
        var next = readings.findNextEffective(candidate.vehicleId(), candidate.readingType(), epoch,
                candidate.recordedAt()).orElse(null);
        var sameTime = readings.findEffectiveAt(candidate.vehicleId(), candidate.readingType(), epoch,
                candidate.recordedAt());
        rejectDuplicateManual(candidate, sameTime);
        chronology.validate(candidate, previous, next, sameTime);

        var saved = readings.save(candidate);
        synchronizeSnapshot(vehicle, saved);
        events.publishAfterCommit(new VehicleReadingRecorded(saved.id(), saved.vehicleId(),
                saved.readingType().name(), saved.value(), saved.unit().name(), saved.sourceType().name(),
                saved.sourceReferenceId(), saved.recordedAt(), saved.receivedAt()));
        return saved;
    }

    private void validateCommand(RecordCommand command) {
        if (command.vehicleId() == null) invalid("Vehicle id is required");
        if (command.readingType() == null) invalid("Reading type is required");
        if (command.value() == null) invalid("Reading value is required");
        if (command.sourceType() == null) invalid("Reading source type is required");
        if (command.recordedAt() == null) invalid("Recorded time is required");
        if (command.actorId() == null) invalid("Created-by user is required");
        if (command.sourceType() == VehicleReadingSourceType.MANUAL) {
            if (command.sourceReferenceId() != null) invalid("Manual readings cannot have a source reference");
            if (blank(command.idempotencyKey())) invalid("Manual readings require an idempotency key");
            if (command.recordedAt().isAfter(now().plusMinutes(5))) {
                invalid("Manual recorded time cannot be more than five minutes in the future");
            }
        } else {
            if (command.sourceReferenceId() == null) invalid("System readings require a source reference");
            if (command.sourceType() == VehicleReadingSourceType.METER_RESET
                    || command.sourceType() == VehicleReadingSourceType.TELEMATICS
                    || command.sourceType() == VehicleReadingSourceType.MAINTENANCE) {
                invalid("Reading source is reserved for a later workflow");
            }
        }
    }

    private java.util.Optional<VehicleReading> sourceReplay(RecordCommand command) {
        if (command.sourceReferenceId() == null) return java.util.Optional.empty();
        return readings.findOriginalBySource(command.vehicleId(), command.readingType(), command.sourceType(),
                command.sourceReferenceId());
    }

    private VehicleReading requireEquivalentReplay(VehicleReading existing, RecordCommand command,
                                                   BigDecimal normalized) {
        if (!existing.vehicleId().equals(command.vehicleId())
                || existing.readingType() != command.readingType()
                || existing.value().compareTo(normalized) != 0
                || existing.sourceType() != command.sourceType()
                || !Objects.equals(existing.sourceReferenceId(), command.sourceReferenceId())
                || !existing.recordedAt().isEqual(command.recordedAt())) {
            throw new ConflictException("DUPLICATE_VEHICLE_READING",
                    "Reading idempotency/source identity was already used with different facts");
        }
        return existing;
    }

    private void rejectDuplicateManual(VehicleReading candidate, java.util.List<VehicleReading> sameTime) {
        if (candidate.sourceType() == VehicleReadingSourceType.MANUAL && sameTime.stream().anyMatch(reading ->
                reading.sourceType() == VehicleReadingSourceType.MANUAL
                        && reading.value().compareTo(candidate.value()) == 0)) {
            throw new ConflictException("DUPLICATE_VEHICLE_READING",
                    "An equivalent manual reading already exists at the same recorded time");
        }
    }

    private void synchronizeSnapshot(Vehicle vehicle, VehicleReading saved) {
        var latest = readings.findLatestEffective(saved.vehicleId(), saved.readingType(), saved.meterEpoch())
                .orElse(saved);
        var updated = saved.readingType() == VehicleReadingType.ODOMETER
                ? copySnapshot(vehicle, latest.value().doubleValue(), vehicle.engineHours())
                : copySnapshot(vehicle, vehicle.currentOdometerKm(), latest.value().doubleValue());
        vehicles.save(updated);
    }

    private Vehicle copySnapshot(Vehicle vehicle, Double odometer, Double engineHours) {
        return new Vehicle(vehicle.id(), vehicle.registrationNumber(), vehicle.chassisNumber(), vehicle.engineNumber(),
                vehicle.categoryId(), vehicle.typeId(), vehicle.manufacturer(), vehicle.model(),
                vehicle.manufactureYear(), vehicle.ownershipType(), vehicle.operationalStatus(), odometer,
                engineHours, vehicle.capacityKg(), vehicle.active());
    }

    private Vehicle requireVehicle(UUID vehicleId) {
        if (vehicleId == null) throw new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: null");
        return vehicles.findById(vehicleId).orElseThrow(() ->
                new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: " + vehicleId));
    }

    private String idempotencyKey(RecordCommand command) {
        if (command.sourceType() == VehicleReadingSourceType.MANUAL) return command.idempotencyKey().trim();
        return command.sourceType().name() + ":" + command.sourceReferenceId() + ":" + command.readingType().name();
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) invalid("Reading value is required");
        if (value.signum() < 0) invalid("Reading value cannot be negative");
        var normalized = value.setScale(3, RoundingMode.HALF_UP);
        if (normalized.precision() > 19) invalid("Reading value exceeds NUMERIC(19,3)");
        return normalized;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void invalid(String message) {
        throw new BusinessRuleException("INVALID_VEHICLE_READING", message);
    }

    private static com.transportlogistics.app.fleet.domain.model.VehicleReadingType domainType(ReadingType type) {
        if (type == null) invalid("Reading type is required");
        return com.transportlogistics.app.fleet.domain.model.VehicleReadingType.valueOf(type.name());
    }

    private static VehicleReadingSourceType domainSource(SourceType source) {
        if (source == null) invalid("Reading source type is required");
        return VehicleReadingSourceType.valueOf(source.name());
    }

    private static ReadingType publicType(com.transportlogistics.app.fleet.domain.model.VehicleReadingType type) {
        return ReadingType.valueOf(type.name());
    }

    private static SourceType publicSource(VehicleReadingSourceType source) {
        return SourceType.valueOf(source.name());
    }
}
