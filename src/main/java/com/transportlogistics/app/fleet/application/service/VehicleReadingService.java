package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.CoverageStatus;
import com.transportlogistics.app.fleet.TripDistanceStatus;
import com.transportlogistics.app.fleet.TripDistanceSummary;
import com.transportlogistics.app.fleet.VehicleMeterResetRecorded;
import com.transportlogistics.app.fleet.VehicleMileageQuery;
import com.transportlogistics.app.fleet.VehicleMileageSummary;
import com.transportlogistics.app.fleet.ManualVehicleReadingRecorder;
import com.transportlogistics.app.fleet.VehicleReadingCorrected;
import com.transportlogistics.app.fleet.VehicleReadingRecorded;
import com.transportlogistics.app.fleet.VehicleReadingRecorder;
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
import com.transportlogistics.app.fleet.domain.service.VehicleReadingChronologyPolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class VehicleReadingService implements VehicleReadingUseCase, VehicleReadingRecorder,
        ManualVehicleReadingRecorder, VehicleMileageQuery {
    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal MAX_SPEED_KM_PER_HOUR = new BigDecimal("200.0");
    private static final BigDecimal MAX_DAILY_KM_JUMP = new BigDecimal("5000.0");

    private final VehicleRepository vehicles;
    private final VehicleReadingRepository readings;
    private final VehicleMeterResetRepository meterResets;
    private final VehicleReadingTransaction transactions;
    private final VehicleReadingEventPublisher events;
    private final Clock clock;
    private final VehicleReadingChronologyPolicy chronology = new VehicleReadingChronologyPolicy();

    public VehicleReadingService(VehicleRepository vehicles, VehicleReadingRepository readings,
                                 VehicleReadingTransaction transactions, VehicleReadingEventPublisher events,
                                 Clock clock) {
        this(vehicles, readings, noOpMeterResetRepository(), transactions, events, clock);
    }

    public VehicleReadingService(VehicleRepository vehicles, VehicleReadingRepository readings,
                                 VehicleMeterResetRepository meterResets,
                                 VehicleReadingTransaction transactions, VehicleReadingEventPublisher events,
                                 Clock clock) {
        this.vehicles = vehicles;
        this.readings = readings;
        this.meterResets = meterResets == null ? noOpMeterResetRepository() : meterResets;
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
    public VehicleReading correct(CorrectCommand command) {
        if (command == null) invalid("Correction command is required");
        return transactions.execute(() -> correctLocked(command));
    }

    @Override
    public VehicleMeterReset resetMeter(ResetMeterCommand command) {
        if (command == null) invalid("Reset meter command is required");
        return transactions.execute(() -> resetMeterLocked(command));
    }

    @Override
    public List<VehicleMeterReset> listMeterResets(UUID vehicleId) {
        requireVehicle(vehicleId);
        return meterResets.findByVehicleId(vehicleId);
    }

    @Override
    public VehicleReadingRecorder.Result record(VehicleReadingRecorder.Command command) {
        if (command == null) invalid("Reading command is required");
        var reading = record(new RecordCommand(command.vehicleId(), domainType(command.readingType()),
                command.value(), domainSource(command.sourceType()), command.sourceReferenceId(),
                command.recordedAt(), command.actorId(), null, null));
        return new VehicleReadingRecorder.Result(reading.id(), reading.vehicleId(), publicType(reading.readingType()), reading.value(),
                reading.unit().name(), publicSource(reading.sourceType()), reading.sourceReferenceId(),
                reading.recordedAt(), reading.receivedAt());
    }

    @Override
    public ManualVehicleReadingRecorder.Result recordManual(ManualVehicleReadingRecorder.Command command) {
        if (command == null) invalid("Reading command is required");
        var reading = record(new RecordCommand(command.vehicleId(),
                VehicleReadingType.valueOf(command.readingType().name()), command.value(),
                VehicleReadingSourceType.MANUAL, null, command.recordedAt(), command.actorId(),
                command.idempotencyKey(), command.notes()));
        return new ManualVehicleReadingRecorder.Result(reading.id(), reading.vehicleId(),
                ManualVehicleReadingRecorder.ReadingType.valueOf(reading.readingType().name()),
                reading.value(), reading.recordedAt());
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

    @Override
    public VehicleMileageSummary getMileage(UUID vehicleId, OffsetDateTime from, OffsetDateTime to) {
        requireVehicle(vehicleId);
        if (from != null && to != null && to.isBefore(from)) {
            invalid("Date range is invalid: 'to' cannot be before 'from'");
        }

        var allReadings = readings.search(new SearchQuery(vehicleId, null, null, null, null, 0, 1000)).content();
        var odoReadings = allReadings.stream()
                .filter(r -> r.readingType() == VehicleReadingType.ODOMETER)
                .filter(r -> inRange(r.recordedAt(), from, to))
                .sorted(Comparator.comparing(VehicleReading::recordedAt))
                .toList();

        var engineReadings = allReadings.stream()
                .filter(r -> r.readingType() == VehicleReadingType.ENGINE_HOURS)
                .filter(r -> inRange(r.recordedAt(), from, to))
                .sorted(Comparator.comparing(VehicleReading::recordedAt))
                .toList();

        var resets = meterResets.findByVehicleId(vehicleId).stream()
                .filter(r -> inRange(r.effectiveAt(), from, to))
                .toList();

        BigDecimal openingOdo = odoReadings.isEmpty() ? null : odoReadings.getFirst().value();
        BigDecimal closingOdo = odoReadings.isEmpty() ? null : odoReadings.getLast().value();
        BigDecimal distanceTravelled = calculateDistanceAcrossEpochs(odoReadings);

        BigDecimal openingEngine = engineReadings.isEmpty() ? null : engineReadings.getFirst().value();
        BigDecimal closingEngine = engineReadings.isEmpty() ? null : engineReadings.getLast().value();
        BigDecimal engineHoursUsed = calculateHoursAcrossEpochs(engineReadings);

        CoverageStatus coverage = determineCoverage(odoReadings, engineReadings, from, to);
        boolean abnormal = detectAbnormalReadings(odoReadings);

        return new VehicleMileageSummary(
                vehicleId,
                from,
                to,
                openingOdo,
                closingOdo,
                distanceTravelled,
                openingEngine,
                closingEngine,
                engineHoursUsed,
                resets.size(),
                coverage,
                abnormal
        );
    }

    @Override
    public TripDistanceSummary calculateTripDistance(UUID tripId) {
        if (tripId == null) invalid("Trip id is required");
        var allReadings = readings.search(new SearchQuery(null, VehicleReadingType.ODOMETER, null, null, null, 0, 1000)).content();
        var startReading = allReadings.stream()
                .filter(r -> r.sourceType() == VehicleReadingSourceType.TRIP_START && tripId.equals(r.sourceReferenceId()))
                .findFirst();
        var endReading = allReadings.stream()
                .filter(r -> r.sourceType() == VehicleReadingSourceType.TRIP_END && tripId.equals(r.sourceReferenceId()))
                .findFirst();

        if (startReading.isEmpty() && endReading.isEmpty()) {
            return new TripDistanceSummary(tripId, null, null, null, BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP), TripDistanceStatus.MISMATCH);
        }
        if (startReading.isEmpty()) {
            return new TripDistanceSummary(tripId, endReading.get().vehicleId(), null, endReading.get().value(),
                    BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP), TripDistanceStatus.PENDING_START);
        }
        if (endReading.isEmpty()) {
            return new TripDistanceSummary(tripId, startReading.get().vehicleId(), startReading.get().value(), null,
                    BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP), TripDistanceStatus.PENDING_END);
        }

        var start = startReading.get();
        var end = endReading.get();
        BigDecimal distance;
        if (start.meterEpoch() == end.meterEpoch()) {
            distance = end.value().subtract(start.value());
        } else {
            var resets = meterResets.findByVehicleIdAndType(start.vehicleId(), VehicleReadingType.ODOMETER);
            distance = calculateDistanceWithResets(start, end, resets);
        }

        return new TripDistanceSummary(tripId, start.vehicleId(), start.value(), end.value(),
                distance.setScale(3, RoundingMode.HALF_UP), TripDistanceStatus.CALCULATED);
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

    private VehicleReading correctLocked(CorrectCommand command) {
        if (command.vehicleId() == null) invalid("Vehicle id is required");
        if (command.originalReadingId() == null) invalid("Original reading id is required");
        if (command.correctedValue() == null) invalid("Corrected value is required");
        if (command.correctedValue().compareTo(BigDecimal.ZERO) < 0) invalid("Corrected value cannot be negative");
        if (blank(command.reason())) invalid("Correction reason is required");
        if (command.actorId() == null) invalid("Actor id is required");

        var vehicle = vehicles.findByIdForUpdate(command.vehicleId()).orElseThrow(() ->
                new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: " + command.vehicleId()));

        var original = readings.findById(command.originalReadingId()).orElseThrow(() ->
                new NotFoundException("VEHICLE_READING_NOT_FOUND", "Original vehicle reading not found: " + command.originalReadingId()));

        if (!original.vehicleId().equals(command.vehicleId())) {
            throw new ConflictException("VEHICLE_MISMATCH", "Original reading does not belong to vehicle: " + command.vehicleId());
        }
        if (readings.findCorrection(original.id()).isPresent()) {
            throw new ConflictException("DUPLICATE_VEHICLE_READING_CORRECTION", "This reading has already been corrected");
        }

        var normalized = normalize(command.correctedValue());
        var recordedAt = command.recordedAt() != null ? command.recordedAt() : original.recordedAt();
        var now = now();

        var correction = new VehicleReading(
                UUID.randomUUID(),
                original.vehicleId(),
                original.readingType(),
                normalized,
                original.unit(),
                original.meterEpoch(),
                original.sourceType(),
                original.sourceReferenceId(),
                recordedAt,
                now,
                command.actorId(),
                original.id(),
                command.reason().trim(),
                null,
                original.notes(),
                now
        );

        var previous = readings.findPreviousEffective(correction.vehicleId(), correction.readingType(),
                correction.meterEpoch(), correction.recordedAt())
                .filter(r -> !r.id().equals(original.id()))
                .orElse(null);
        var next = readings.findNextEffective(correction.vehicleId(), correction.readingType(),
                correction.meterEpoch(), correction.recordedAt())
                .filter(r -> !r.id().equals(original.id()))
                .orElse(null);
        var sameTime = readings.findEffectiveAt(correction.vehicleId(), correction.readingType(),
                correction.meterEpoch(), correction.recordedAt()).stream()
                .filter(r -> !r.id().equals(original.id()))
                .toList();

        chronology.validate(correction, previous, next, sameTime);

        var saved = readings.save(correction);
        synchronizeSnapshot(vehicle, saved);

        events.publishAfterCommit(new VehicleReadingCorrected(saved.id(), original.id(), saved.vehicleId(),
                saved.readingType().name(), saved.value(), command.actorId(), now));

        return saved;
    }

    private VehicleMeterReset resetMeterLocked(ResetMeterCommand command) {
        if (command.vehicleId() == null) invalid("Vehicle id is required");
        if (command.readingType() == null) invalid("Reading type is required");
        if (command.newMeterValue() == null) invalid("New meter value is required");
        if (command.newMeterValue().compareTo(BigDecimal.ZERO) < 0) invalid("New meter value cannot be negative");
        if (command.effectiveAt() == null) invalid("Effective time is required");
        if (blank(command.reason())) invalid("Meter reset reason is required");
        if (command.actorId() == null) invalid("Actor id is required");

        var vehicle = vehicles.findByIdForUpdate(command.vehicleId()).orElseThrow(() ->
                new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: " + command.vehicleId()));
        if (!vehicle.active()) {
            invalid("Inactive vehicles cannot undergo meter reset");
        }

        var now = now();
        if (command.effectiveAt().isAfter(now.plusMinutes(5))) {
            invalid("Meter reset effective time cannot be more than 5 minutes in the future");
        }

        int fromEpoch = readings.findCurrentMeterEpoch(command.vehicleId(), command.readingType());
        int toEpoch = fromEpoch + 1;

        var lastReadingOpt = readings.findLatestEffective(command.vehicleId(), command.readingType(), fromEpoch);
        BigDecimal lastValue = lastReadingOpt.map(VehicleReading::value).orElse(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));

        var normalizedNewValue = normalize(command.newMeterValue());

        var reset = new VehicleMeterReset(
                UUID.randomUUID(),
                command.vehicleId(),
                command.readingType(),
                fromEpoch,
                toEpoch,
                lastValue,
                normalizedNewValue,
                command.effectiveAt(),
                command.reason().trim(),
                command.actorId(),
                now
        );

        var savedReset = meterResets.save(reset);

        var initialReading = new VehicleReading(
                UUID.randomUUID(),
                command.vehicleId(),
                command.readingType(),
                normalizedNewValue,
                command.readingType().unit(),
                toEpoch,
                VehicleReadingSourceType.METER_RESET,
                savedReset.id(),
                command.effectiveAt(),
                now,
                command.actorId(),
                null,
                null,
                "METER_RESET:" + savedReset.id() + ":" + command.readingType(),
                "Meter reset: " + command.reason().trim(),
                now
        );

        var savedReading = readings.save(initialReading);
        synchronizeSnapshot(vehicle, savedReading);

        events.publishAfterCommit(new VehicleMeterResetRecorded(savedReset.id(), savedReset.vehicleId(),
                savedReset.readingType().name(), savedReset.fromEpoch(), savedReset.toEpoch(),
                savedReset.lastReadingValue(), savedReset.newMeterValue(), savedReset.effectiveAt()));

        return savedReset;
    }

    private BigDecimal calculateDistanceAcrossEpochs(List<VehicleReading> readings) {
        if (readings == null || readings.size() < 2) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        Map<Integer, List<VehicleReading>> byEpoch = readings.stream()
                .collect(Collectors.groupingBy(VehicleReading::meterEpoch));
        BigDecimal total = BigDecimal.ZERO;
        for (List<VehicleReading> epochReadings : byEpoch.values()) {
            if (epochReadings.size() >= 2) {
                var sorted = epochReadings.stream().sorted(Comparator.comparing(VehicleReading::recordedAt)).toList();
                BigDecimal delta = sorted.getLast().value().subtract(sorted.getFirst().value());
                if (delta.compareTo(BigDecimal.ZERO) > 0) {
                    total = total.add(delta);
                }
            }
        }
        return total.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateHoursAcrossEpochs(List<VehicleReading> readings) {
        if (readings == null || readings.size() < 2) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        Map<Integer, List<VehicleReading>> byEpoch = readings.stream()
                .collect(Collectors.groupingBy(VehicleReading::meterEpoch));
        BigDecimal total = BigDecimal.ZERO;
        for (List<VehicleReading> epochReadings : byEpoch.values()) {
            if (epochReadings.size() >= 2) {
                var sorted = epochReadings.stream().sorted(Comparator.comparing(VehicleReading::recordedAt)).toList();
                BigDecimal delta = sorted.getLast().value().subtract(sorted.getFirst().value());
                if (delta.compareTo(BigDecimal.ZERO) > 0) {
                    total = total.add(delta);
                }
            }
        }
        return total.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDistanceWithResets(VehicleReading start, VehicleReading end, List<VehicleMeterReset> resets) {
        BigDecimal distance = BigDecimal.ZERO;
        int currentEpoch = start.meterEpoch();
        BigDecimal currentStartValue = start.value();

        while (currentEpoch < end.meterEpoch()) {
            int epoch = currentEpoch;
            var resetOpt = resets.stream().filter(r -> r.fromEpoch() == epoch).findFirst();
            if (resetOpt.isPresent()) {
                var reset = resetOpt.get();
                distance = distance.add(reset.lastReadingValue().subtract(currentStartValue));
                currentStartValue = reset.newMeterValue();
                currentEpoch = reset.toEpoch();
            } else {
                break;
            }
        }
        distance = distance.add(end.value().subtract(currentStartValue));
        return distance.max(BigDecimal.ZERO);
    }

    private CoverageStatus determineCoverage(List<VehicleReading> odo, List<VehicleReading> engine,
                                             OffsetDateTime from, OffsetDateTime to) {
        if (odo.isEmpty() && engine.isEmpty()) return CoverageStatus.NO_DATA;
        if (odo.size() < 2 && engine.size() < 2) return CoverageStatus.PARTIAL;
        return CoverageStatus.COMPLETE;
    }

    private boolean detectAbnormalReadings(List<VehicleReading> readings) {
        if (readings == null || readings.size() < 2) return false;
        for (int i = 0; i < readings.size() - 1; i++) {
            var first = readings.get(i);
            var second = readings.get(i + 1);
            if (first.meterEpoch() == second.meterEpoch()) {
                BigDecimal delta = second.value().subtract(first.value());
                if (delta.compareTo(MAX_DAILY_KM_JUMP) > 0) return true;
                long seconds = Duration.between(first.recordedAt(), second.recordedAt()).abs().toSeconds();
                if (seconds > 0) {
                    BigDecimal hours = BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
                    if (hours.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal speed = delta.divide(hours, 2, RoundingMode.HALF_UP);
                        if (speed.compareTo(MAX_SPEED_KM_PER_HOUR) > 0) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean inRange(OffsetDateTime timestamp, OffsetDateTime from, OffsetDateTime to) {
        if (timestamp == null) return false;
        if (from != null && timestamp.isBefore(from)) return false;
        if (to != null && timestamp.isAfter(to)) return false;
        return true;
    }

    private void validateCommand(RecordCommand command) {
        if (command.vehicleId() == null) invalid("Vehicle id is required");
        if (command.readingType() == null) invalid("Reading type is required");
        if (command.value() == null) invalid("Reading value is required");
        if (command.sourceType() == null) invalid("Reading source type is required");
        if (command.recordedAt() == null) invalid("Recorded time is required");
        if (command.actorId() == null) invalid("Created-by user is required");
        if (command.sourceType() == VehicleReadingSourceType.MANUAL || command.sourceType() == VehicleReadingSourceType.BASELINE) {
            if (command.sourceType() == VehicleReadingSourceType.MANUAL && command.sourceReferenceId() != null) {
                invalid("Manual readings cannot have a source reference");
            }
            if (blank(command.idempotencyKey())) invalid("Manual readings require an idempotency key");
            if (command.recordedAt().isAfter(now().plusMinutes(5))) {
                invalid("Manual recorded time cannot be more than five minutes in the future");
            }
        } else {
            if (command.sourceReferenceId() == null) invalid("System readings require a source reference");
            if (command.sourceType() == VehicleReadingSourceType.TELEMATICS
                    || command.sourceType() == VehicleReadingSourceType.MAINTENANCE) {
                invalid("Reading source is reserved for a later workflow");
            }
        }
    }

    private Optional<VehicleReading> sourceReplay(RecordCommand command) {
        if (command.sourceReferenceId() == null) return Optional.empty();
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

    private void rejectDuplicateManual(VehicleReading candidate, List<VehicleReading> sameTime) {
        if (candidate.sourceType() == VehicleReadingSourceType.MANUAL && sameTime.stream().anyMatch(reading ->
                reading.sourceType() == VehicleReadingSourceType.MANUAL
                        && reading.value().compareTo(candidate.value()) == 0)) {
            throw new ConflictException("DUPLICATE_VEHICLE_READING",
                    "An equivalent manual reading already exists at the same recorded time");
        }
    }

    private void synchronizeSnapshot(Vehicle vehicle, VehicleReading reading) {
        var latest = readings.findLatestEffective(reading.vehicleId(), reading.readingType(), reading.meterEpoch())
                .orElse(reading);
        if (reading.readingType() == VehicleReadingType.ODOMETER) {
            vehicles.save(new Vehicle(vehicle.id(), vehicle.registrationNumber(), vehicle.chassisNumber(),
                    vehicle.engineNumber(), vehicle.categoryId(), vehicle.typeId(), vehicle.manufacturer(),
                    vehicle.model(), vehicle.manufactureYear(), vehicle.ownershipType(), vehicle.operationalStatus(),
                    latest.value().doubleValue(), vehicle.engineHours(), vehicle.capacityKg(), vehicle.active()));
        } else if (reading.readingType() == VehicleReadingType.ENGINE_HOURS) {
            vehicles.save(new Vehicle(vehicle.id(), vehicle.registrationNumber(), vehicle.chassisNumber(),
                    vehicle.engineNumber(), vehicle.categoryId(), vehicle.typeId(), vehicle.manufacturer(),
                    vehicle.model(), vehicle.manufactureYear(), vehicle.ownershipType(), vehicle.operationalStatus(),
                    vehicle.currentOdometerKm(), latest.value().doubleValue(), vehicle.capacityKg(), vehicle.active()));
        }
    }

    private void requireVehicle(UUID vehicleId) {
        if (vehicles.findById(vehicleId).isEmpty()) {
            throw new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: " + vehicleId);
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value.scale() > 3) invalid("Reading values support a maximum of three decimal places");
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    private String idempotencyKey(RecordCommand command) {
        if (command.sourceType() == VehicleReadingSourceType.MANUAL) {
            return command.idempotencyKey().trim();
        }
        return command.sourceType().name() + ":" + command.sourceReferenceId() + ":" + command.readingType().name();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void invalid(String message) {
        throw new BusinessRuleException("INVALID_VEHICLE_READING", message);
    }

    private static VehicleMeterResetRepository noOpMeterResetRepository() {
        return new VehicleMeterResetRepository() {
            @Override public VehicleMeterReset save(VehicleMeterReset reset) { return reset; }
            @Override public List<VehicleMeterReset> findByVehicleId(UUID vehicleId) { return List.of(); }
            @Override public List<VehicleMeterReset> findByVehicleIdAndType(UUID vehicleId, VehicleReadingType readingType) { return List.of(); }
            @Override public Optional<VehicleMeterReset> findLatestByVehicleIdAndType(UUID vehicleId, VehicleReadingType readingType) { return Optional.empty(); }
        };
    }

    private static VehicleReadingType domainType(VehicleReadingRecorder.ReadingType type) {
        return VehicleReadingType.valueOf(type.name());
    }

    private static VehicleReadingRecorder.ReadingType publicType(VehicleReadingType type) {
        return VehicleReadingRecorder.ReadingType.valueOf(type.name());
    }

    private static VehicleReadingSourceType domainSource(VehicleReadingRecorder.SourceType source) {
        return VehicleReadingSourceType.valueOf(source.name());
    }

    private static VehicleReadingRecorder.SourceType publicSource(VehicleReadingSourceType source) {
        return VehicleReadingRecorder.SourceType.valueOf(source.name());
    }
}
