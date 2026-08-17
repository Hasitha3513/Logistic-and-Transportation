package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.*;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.*;
import com.transportlogistics.app.fleet.domain.model.*;
import com.transportlogistics.app.fleet.domain.service.VehicleReadingChronologyPolicy;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public final class VehicleReadingService implements VehicleReadingUseCase, VehicleReadingRecorder, VehicleMileageQuery {
    private static final int MAX_PAGE_SIZE = 100;

    private final VehicleRepository vehicles;
    private final VehicleReadingRepository readings;
    private final VehicleMeterResetRepository resets;
    private final VehicleReadingTransaction transactions;
    private final VehicleReadingEventPublisher events;
    private final Clock clock;
    private final VehicleReadingChronologyPolicy chronology = new VehicleReadingChronologyPolicy();

    public VehicleReadingService(VehicleRepository vehicles, VehicleReadingRepository readings,
                                 VehicleMeterResetRepository resets,
                                 VehicleReadingTransaction transactions, VehicleReadingEventPublisher events,
                                 Clock clock) {
        this.vehicles = vehicles;
        this.readings = readings;
        this.resets = resets;
        this.transactions = transactions;
        this.events = events;
        this.clock = clock;
    }

    public VehicleReadingService(VehicleRepository vehicles, VehicleReadingRepository readings,
                                 VehicleReadingTransaction transactions, VehicleReadingEventPublisher events,
                                 Clock clock) {
        this(vehicles, readings, null, transactions, events, clock);
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
    public VehicleReadingUseCase.LatestReadings latest(UUID vehicleId) {
        requireVehicle(vehicleId);
        var odometerEpoch = readings.findCurrentMeterEpoch(vehicleId, VehicleReadingType.ODOMETER);
        var engineEpoch = readings.findCurrentMeterEpoch(vehicleId, VehicleReadingType.ENGINE_HOURS);
        return new VehicleReadingUseCase.LatestReadings(vehicleId,
                readings.findLatestEffective(vehicleId, VehicleReadingType.ODOMETER, odometerEpoch),
                readings.findLatestEffective(vehicleId, VehicleReadingType.ENGINE_HOURS, engineEpoch));
    }

    @Override
    public java.util.List<VehicleMeterReset> listResets(UUID vehicleId) {
        requireVehicle(vehicleId);
        if (resets == null) return java.util.List.of();
        return resets.findByVehicleId(vehicleId);
    }

    @Override
    public VehicleMileageSummary mileageSummary(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                                boolean includeSourceBreakdown) {
        if (vehicleId == null) invalid("Vehicle id is required");
        if (from == null) invalid("From timestamp is required");
        if (to == null) invalid("To timestamp is required");
        if (from.isAfter(to)) invalid("From timestamp cannot be after to timestamp");
        requireVehicle(vehicleId);

        var odoResult = calculateMetricAcrossEpochs(vehicleId, VehicleReadingType.ODOMETER, from, to);
        var engResult = calculateMetricAcrossEpochs(vehicleId, VehicleReadingType.ENGINE_HOURS, from, to);

        var allPeriodReadings = readings.findAllInPeriod(vehicleId, from, to);
        int readingCount = allPeriodReadings.size();
        int correctionCount = readings.countCorrectionsInPeriod(vehicleId, from, to);
        int resetCount = (int) (resets != null ? resets.findByVehicleId(vehicleId).stream()
                .filter(r -> !r.effectiveAt().isBefore(from) && !r.effectiveAt().isAfter(to))
                .count() : 0);

        OffsetDateTime firstReadingAt = allPeriodReadings.isEmpty() ? null : allPeriodReadings.getFirst().recordedAt();
        OffsetDateTime lastReadingAt = allPeriodReadings.isEmpty() ? null : allPeriodReadings.getLast().recordedAt();

        CoverageStatus coverage = odoResult.coverageStatus() != CoverageStatus.NO_DATA
                ? odoResult.coverageStatus()
                : engResult.coverageStatus();
        String coverageReason = odoResult.coverageStatus() != CoverageStatus.NO_DATA
                ? odoResult.reason()
                : engResult.reason();

        Map<VehicleReadingSourceType, Integer> sourceCounts = Map.of();
        if (includeSourceBreakdown && !allPeriodReadings.isEmpty()) {
            var counts = new EnumMap<VehicleReadingSourceType, Integer>(VehicleReadingSourceType.class);
            for (var r : allPeriodReadings) {
                counts.merge(r.sourceType(), 1, Integer::sum);
            }
            sourceCounts = counts;
        }

        return new VehicleMileageSummary(vehicleId, from, to, odoResult.openingValue(), odoResult.closingValue(),
                odoResult.distance(), engResult.openingValue(), engResult.closingValue(), engResult.distance(),
                readingCount, correctionCount, resetCount, firstReadingAt, lastReadingAt,
                coverage, coverageReason, sourceCounts);
    }

    @Override
    public TripDistanceSummary tripDistance(UUID tripId, UUID vehicleId) {
        if (tripId == null) invalid("Trip id is required");
        if (vehicleId == null) invalid("Vehicle id is required");
        requireVehicle(vehicleId);

        var optStart = readings.findEffectiveBySource(vehicleId, VehicleReadingType.ODOMETER,
                VehicleReadingSourceType.TRIP_START, tripId);
        var optEnd = readings.findEffectiveBySource(vehicleId, VehicleReadingType.ODOMETER,
                VehicleReadingSourceType.TRIP_END, tripId);

        if (optStart.isEmpty() && optEnd.isEmpty()) {
            return new TripDistanceSummary(tripId, vehicleId, null, null, null,
                    TripDistanceStatus.UNAVAILABLE, false, "No trip readings recorded for vehicle");
        }
        if (optStart.isEmpty()) {
            return new TripDistanceSummary(tripId, vehicleId, null, optEnd.get().value(), null,
                    TripDistanceStatus.PARTIAL, false, "Trip completion reading recorded without start reading");
        }
        if (optEnd.isEmpty()) {
            return new TripDistanceSummary(tripId, vehicleId, optStart.get().value(), null, null,
                    TripDistanceStatus.PARTIAL, false, "Trip start reading recorded; trip not yet completed");
        }

        var start = optStart.get();
        var end = optEnd.get();

        if (start.meterEpoch() == end.meterEpoch()) {
            var dist = end.value().subtract(start.value());
            var nonNegDist = dist.signum() < 0 ? BigDecimal.ZERO.setScale(3) : dist.setScale(3, RoundingMode.HALF_UP);
            return new TripDistanceSummary(tripId, vehicleId, start.value(), end.value(), nonNegDist,
                    TripDistanceStatus.AVAILABLE, false, null);
        } else if (end.meterEpoch() > start.meterEpoch()) {
            var epochRes = calculateMetricAcrossEpochs(vehicleId, VehicleReadingType.ODOMETER,
                    start.recordedAt(), end.recordedAt());
            return new TripDistanceSummary(tripId, vehicleId, start.value(), end.value(), epochRes.distance(),
                    TripDistanceStatus.AVAILABLE, true, "Meter reset occurred during trip");
        } else {
            return new TripDistanceSummary(tripId, vehicleId, start.value(), end.value(), BigDecimal.ZERO.setScale(3),
                    TripDistanceStatus.UNAVAILABLE, false, "Inverted meter epochs on trip readings");
        }
    }

    @Override
    public VehicleMileageSummary getVehicleMileageSummary(UUID vehicleId, OffsetDateTime from, OffsetDateTime to,
                                                          boolean includeSourceBreakdown) {
        return mileageSummary(vehicleId, from, to, includeSourceBreakdown);
    }

    @Override
    public TripDistanceSummary getTripDistance(UUID tripId, UUID vehicleId) {
        return tripDistance(tripId, vehicleId);
    }

    @Override
    public VehicleMileageQuery.LatestReadings getLatestReadings(UUID vehicleId) {
        var domainLatest = latest(vehicleId);
        var odoSnapshot = domainLatest.odometer().map(r -> new VehicleMileageQuery.ReadingSnapshot(
                r.id(), r.value(), r.unit().name(), r.meterEpoch(), r.sourceType().name(), r.recordedAt())).orElse(null);
        var engSnapshot = domainLatest.engineHours().map(r -> new VehicleMileageQuery.ReadingSnapshot(
                r.id(), r.value(), r.unit().name(), r.meterEpoch(), r.sourceType().name(), r.recordedAt())).orElse(null);
        return new VehicleMileageQuery.LatestReadings(vehicleId, odoSnapshot, engSnapshot);
    }

    private MetricCalculationResult calculateMetricAcrossEpochs(UUID vehicleId, VehicleReadingType type,
                                                                OffsetDateTime from, OffsetDateTime to) {
        var optOpening = readings.findOpeningEffective(vehicleId, type, from);
        var optClosing = readings.findClosingEffective(vehicleId, type, to);
        var periodReadings = readings.findEffectiveInPeriod(vehicleId, type, from, to);

        if (optOpening.isEmpty() && optClosing.isEmpty() && periodReadings.isEmpty()) {
            return new MetricCalculationResult(null, null, BigDecimal.ZERO.setScale(3), CoverageStatus.NO_DATA,
                    "No " + type.name().toLowerCase() + " readings found for vehicle");
        }

        var epochs = new TreeSet<Integer>();
        optOpening.ifPresent(r -> epochs.add(r.meterEpoch()));
        optClosing.ifPresent(r -> epochs.add(r.meterEpoch()));
        periodReadings.forEach(r -> epochs.add(r.meterEpoch()));

        if (epochs.isEmpty()) {
            return new MetricCalculationResult(null, null, BigDecimal.ZERO.setScale(3), CoverageStatus.NO_DATA,
                    "No " + type.name().toLowerCase() + " readings found for vehicle");
        }

        BigDecimal totalDistance = BigDecimal.ZERO.setScale(3);
        BigDecimal firstOpeningVal = null;
        BigDecimal finalClosingVal = null;
        CoverageStatus coverage = CoverageStatus.COMPLETE;
        String reason = null;

        if (optOpening.isEmpty()) {
            coverage = CoverageStatus.PARTIAL;
            reason = "No opening reading recorded at or prior to period start date";
        }

        for (int epoch : epochs) {
            final int currentEpoch = epoch;
            var epochPeriodReadings = periodReadings.stream()
                    .filter(r -> r.meterEpoch() == currentEpoch)
                    .toList();

            BigDecimal epochOpen;
            if (optOpening.isPresent() && optOpening.get().meterEpoch() == currentEpoch) {
                epochOpen = optOpening.get().value();
            } else if (!epochPeriodReadings.isEmpty()) {
                epochOpen = epochPeriodReadings.getFirst().value();
            } else {
                continue;
            }

            BigDecimal epochClose;
            if (optClosing.isPresent() && optClosing.get().meterEpoch() == currentEpoch) {
                epochClose = optClosing.get().value();
            } else if (!epochPeriodReadings.isEmpty()) {
                epochClose = epochPeriodReadings.getLast().value();
            } else {
                epochClose = epochOpen;
            }

            if (firstOpeningVal == null) {
                firstOpeningVal = epochOpen;
            }
            finalClosingVal = epochClose;

            BigDecimal epochDist = epochClose.subtract(epochOpen);
            if (epochDist.signum() > 0) {
                totalDistance = totalDistance.add(epochDist);
            }
        }

        return new MetricCalculationResult(firstOpeningVal, finalClosingVal, totalDistance, coverage, reason);
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
        validateCorrectCommand(command);
        var vehicle = vehicles.findByIdForUpdate(command.vehicleId()).orElseThrow(() ->
                new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: " + command.vehicleId()));
        if (!vehicle.active()) {
            invalid("Inactive vehicles cannot receive reading corrections");
        }

        var target = readings.findById(command.readingId()).orElseThrow(() ->
                new NotFoundException("VEHICLE_READING_NOT_FOUND", "Vehicle reading not found: " + command.readingId()));

        if (!target.vehicleId().equals(command.vehicleId())) {
            throw new BusinessRuleException("INVALID_VEHICLE_READING", "Reading does not belong to vehicle: " + command.vehicleId());
        }

        if (readings.isSuperseded(target.id())) {
            throw new ConflictException("VEHICLE_READING_ALREADY_CORRECTED", "This reading has already been superseded by a correction");
        }

        var currentEpoch = readings.findCurrentMeterEpoch(target.vehicleId(), target.readingType());
        if (target.meterEpoch() != currentEpoch) {
            throw new ConflictException("VEHICLE_READING_NOT_CORRECTABLE", "Readings from prior closed meter epochs cannot be corrected");
        }

        var normalized = normalize(command.value());
        var idempotencyKey = command.idempotencyKey() == null || command.idempotencyKey().isBlank()
                ? null
                : command.idempotencyKey().trim();

        if (idempotencyKey != null) {
            var replay = readings.findByIdempotencyKey(idempotencyKey);
            if (replay.isPresent()) {
                var existing = replay.orElseThrow();
                if (existing.correctionOfReadingId() != null && existing.correctionOfReadingId().equals(target.id())
                        && existing.value().compareTo(normalized) == 0) {
                    return existing;
                }
                throw new ConflictException("DUPLICATE_VEHICLE_READING", "Idempotency key already used for a different fact");
            }
        }

        var now = now();
        var candidate = new VehicleReading(UUID.randomUUID(), target.vehicleId(), target.readingType(), normalized,
                target.unit(), target.meterEpoch(), target.sourceType(), target.sourceReferenceId(),
                target.recordedAt(), now, command.actorId(), target.id(), command.reason().trim(),
                idempotencyKey, command.notes() == null || command.notes().isBlank() ? null : command.notes().trim(), now);

        var previous = readings.findPreviousEffective(candidate.vehicleId(), candidate.readingType(), candidate.meterEpoch(),
                candidate.recordedAt()).orElse(null);
        var next = readings.findNextEffective(candidate.vehicleId(), candidate.readingType(), candidate.meterEpoch(),
                candidate.recordedAt()).orElse(null);
        var sameTime = readings.findEffectiveAt(candidate.vehicleId(), candidate.readingType(), candidate.meterEpoch(),
                candidate.recordedAt()).stream().filter(r -> !r.id().equals(target.id())).toList();

        chronology.validate(candidate, previous, next, sameTime);

        var saved = readings.save(candidate);
        synchronizeSnapshot(vehicle, saved);
        events.publishAfterCommit(new VehicleReadingRecorded(saved.id(), saved.vehicleId(),
                saved.readingType().name(), saved.value(), saved.unit().name(), saved.sourceType().name(),
                saved.sourceReferenceId(), saved.recordedAt(), saved.receivedAt()));
        events.publishAfterCommit(new VehicleReadingCorrected(saved.id(), target.id(), saved.vehicleId(),
                saved.readingType().name(), saved.value(), target.value(), saved.unit().name(),
                saved.correctionReason(), saved.recordedAt(), saved.receivedAt(), saved.createdBy()));
        return saved;
    }

    private VehicleMeterReset resetMeterLocked(ResetMeterCommand command) {
        validateResetCommand(command);
        if (resets == null) {
            throw new IllegalStateException("VehicleMeterResetRepository is not configured");
        }
        var vehicle = vehicles.findByIdForUpdate(command.vehicleId()).orElseThrow(() ->
                new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found: " + command.vehicleId()));
        if (!vehicle.active()) {
            invalid("Inactive vehicles cannot receive meter resets");
        }

        var normalized = normalize(command.newMeterValue());
        var currentEpoch = readings.findCurrentMeterEpoch(command.vehicleId(), command.readingType());
        var latest = readings.findLatestEffective(command.vehicleId(), command.readingType(), currentEpoch);

        UUID previousReadingId = null;
        BigDecimal previousMeterValue = BigDecimal.ZERO.setScale(3);

        if (latest.isPresent()) {
            var prev = latest.orElseThrow();
            if (command.effectiveAt().isBefore(prev.recordedAt())) {
                throw new ConflictException("METER_RESET_CONFLICT",
                        "Meter reset cannot be backdated before an existing effective reading: " + prev.recordedAt());
            }
            previousReadingId = prev.id();
            previousMeterValue = prev.value();
        }

        var newEpoch = currentEpoch + 1;
        var resetId = UUID.randomUUID();
        var now = now();

        var candidateReading = new VehicleReading(UUID.randomUUID(), command.vehicleId(), command.readingType(),
                normalized, command.readingType().unit(), newEpoch, VehicleReadingSourceType.METER_RESET,
                resetId, command.effectiveAt(), now, command.actorId(), null, null,
                "METER_RESET:" + resetId + ":" + command.readingType().name(),
                command.notes() == null || command.notes().isBlank() ? null : command.notes().trim(), now);

        var savedReading = readings.save(candidateReading);

        var meterReset = new VehicleMeterReset(resetId, command.vehicleId(), command.readingType(),
                previousReadingId, previousMeterValue, savedReading.id(), normalized, command.effectiveAt(),
                command.reason().trim(), command.actorId(),
                command.approvedBy() != null ? command.approvedBy() : command.actorId(),
                command.notes() == null || command.notes().isBlank() ? null : command.notes().trim(), now);

        var savedReset = resets.save(meterReset);
        synchronizeSnapshot(vehicle, savedReading);

        events.publishAfterCommit(new VehicleReadingRecorded(savedReading.id(), savedReading.vehicleId(),
                savedReading.readingType().name(), savedReading.value(), savedReading.unit().name(),
                savedReading.sourceType().name(), savedReading.sourceReferenceId(), savedReading.recordedAt(),
                savedReading.receivedAt()));
        events.publishAfterCommit(new VehicleMeterResetRecorded(savedReset.id(), savedReset.vehicleId(),
                savedReset.readingType().name(), savedReset.previousReadingId(), savedReset.previousMeterValue(),
                savedReset.newReadingId(), savedReset.newMeterValue(), savedReset.effectiveAt(),
                savedReset.reason(), savedReset.createdBy(), savedReset.approvedBy(), savedReset.createdAt()));

        return savedReset;
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

    private void validateCorrectCommand(CorrectCommand command) {
        if (command.vehicleId() == null) invalid("Vehicle id is required");
        if (command.readingId() == null) invalid("Reading id is required");
        if (command.value() == null) invalid("Reading value is required");
        if (blank(command.reason())) invalid("Correction reason is required");
        if (command.actorId() == null) invalid("Created-by user is required");
    }

    private void validateResetCommand(ResetMeterCommand command) {
        if (command.vehicleId() == null) invalid("Vehicle id is required");
        if (command.readingType() == null) invalid("Reading type is required");
        if (command.newMeterValue() == null) invalid("New meter value is required");
        if (command.effectiveAt() == null) invalid("Effective time is required");
        if (blank(command.reason())) invalid("Reset reason is required");
        if (command.actorId() == null) invalid("Created-by user is required");
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

    private record MetricCalculationResult(BigDecimal openingValue, BigDecimal closingValue, BigDecimal distance,
                                           CoverageStatus coverageStatus, String reason) {
    }
}
