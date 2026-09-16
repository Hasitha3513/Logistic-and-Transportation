package com.transportlogistics.app.tracking.domain.gpsedge;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Assessment;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Connectivity;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EvaluationContext;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Observation;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Ordering;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Quality;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ReliabilityState;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.SignalState;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Trust;
import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Objects;

public final class GpsReliabilityPolicy {
    public static final Duration FRESH_LIMIT = Duration.ofSeconds(60);
    public static final Duration DETECTOR_AGE_LIMIT = Duration.ofMinutes(5);
    public static final Duration OFFLINE_LIMIT = Duration.ofMinutes(5);
    public static final Duration FUTURE_TOLERANCE = Duration.ofSeconds(120);
    public static final Duration LATE_THRESHOLD = Duration.ofHours(24);
    public static final Duration IMPOSSIBLE_MOVEMENT_WINDOW = Duration.ofMinutes(10);
    public static final double IMPOSSIBLE_MOVEMENT_DISTANCE_METERS = 2_000;
    public static final double IMPOSSIBLE_MOVEMENT_SPEED_KPH = 250;
    public static final int REQUIRED_RECOVERY_POINTS = 2;

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private GpsReliabilityPolicy() {
    }

    public static Assessment assess(Observation observation, EvaluationContext context) {
        Objects.requireNonNull(observation, "observation is required");
        Objects.requireNonNull(context, "context is required");
        if (observation.sourceTimestamp().isBefore(context.retainedAfter())) {
            throw new GpsEdgeCaseException("OUTSIDE_RETENTION", "Observation predates retained history");
        }

        EnumSet<Quality> qualities = EnumSet.noneOf(Quality.class);
        Ordering ordering = ordering(observation, context.latestTrusted());
        classifyAccuracy(observation.accuracyMeters(), qualities);
        classifyTime(observation.sourceTimestamp(), context.evaluatedAt(), qualities);
        if (observation.coordinate().isNullIsland()) {
            qualities.add(Quality.NULL_ISLAND_SUSPECT);
        }
        if (ordering == Ordering.OUT_OF_ORDER) {
            qualities.add(Quality.OUT_OF_ORDER);
        }
        if (!context.bindingValid()) {
            qualities.add(Quality.BINDING_INVALID);
        }
        if (observation.tamperState() == SignalState.ACTIVE) {
            qualities.add(Quality.TAMPER_ACTIVE);
        }
        if (isImpossibleMovement(context.latestTrusted(), observation)) {
            qualities.add(Quality.IMPOSSIBLE_MOVEMENT);
        }
        classifyBattery(observation.batteryPercentage(), qualities);
        if (qualities.isEmpty()) {
            qualities.add(Quality.NORMAL);
        }

        Connectivity connectivity = connectivity(observation, context.evaluatedAt());
        Trust trust = trust(qualities, context.recoveryHold());
        boolean latestEligible = trust == Trust.TRUSTED && ordering == Ordering.IN_ORDER
                && !observation.sourceTimestamp().isAfter(context.evaluatedAt());
        Duration sourceAge = Duration.between(observation.sourceTimestamp(), context.evaluatedAt());
        boolean detectorEligible = latestEligible && !sourceAge.isNegative()
                && sourceAge.compareTo(DETECTOR_AGE_LIMIT) <= 0;
        return new Assessment(trust, ordering, connectivity,
                state(trust, connectivity, qualities, context.recoveryHold()), qualities,
                latestEligible, detectorEligible);
    }

    public static boolean isRapidBatteryDrain(
            BigDecimal previousPercentage,
            Instant previousAt,
            BigDecimal currentPercentage,
            Instant currentAt) {
        if (previousPercentage == null || currentPercentage == null || previousAt == null || currentAt == null
                || currentAt.isBefore(previousAt)) {
            return false;
        }
        return Duration.between(previousAt, currentAt).compareTo(Duration.ofMinutes(30)) <= 0
                && previousPercentage.subtract(currentPercentage).compareTo(BigDecimal.valueOf(20)) >= 0;
    }

    public static boolean isImpossibleMovement(Observation previous, Observation current) {
        if (previous == null || current == null) {
            return false;
        }
        Duration elapsed = Duration.between(previous.sourceTimestamp(), current.sourceTimestamp());
        double distance = distanceMeters(previous.coordinate(), current.coordinate());
        if (elapsed.isZero()) {
            return distance > 0;
        }
        if (elapsed.isNegative() || elapsed.compareTo(IMPOSSIBLE_MOVEMENT_WINDOW) > 0
                || distance < IMPOSSIBLE_MOVEMENT_DISTANCE_METERS) {
            return false;
        }
        double speedKph = distance / elapsed.toMillis() * 3_600;
        return speedKph > IMPOSSIBLE_MOVEMENT_SPEED_KPH;
    }

    static double distanceMeters(GpsCoordinate first, GpsCoordinate second) {
        double firstLatitude = Math.toRadians(first.latitude().doubleValue());
        double secondLatitude = Math.toRadians(second.latitude().doubleValue());
        double deltaLatitude = secondLatitude - firstLatitude;
        double deltaLongitude = Math.toRadians(second.longitude().doubleValue() - first.longitude().doubleValue());
        double haversine = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.pow(Math.sin(deltaLongitude / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
    }

    private static Ordering ordering(Observation observation, Observation latestTrusted) {
        if (latestTrusted == null || observation.sourceTimestamp().isAfter(latestTrusted.sourceTimestamp())) {
            return Ordering.IN_ORDER;
        }
        if (observation.sourceTimestamp().isBefore(latestTrusted.sourceTimestamp())) {
            return Ordering.OUT_OF_ORDER;
        }
        return observation.eventId().toString().compareTo(latestTrusted.eventId().toString()) > 0
                ? Ordering.IN_ORDER : Ordering.EQUAL_SOURCE_TIME;
    }

    private static void classifyAccuracy(Double accuracy, EnumSet<Quality> qualities) {
        if (accuracy == null) {
            qualities.add(Quality.ACCURACY_UNKNOWN);
        } else if (accuracy > 1_000) {
            qualities.add(Quality.UNUSABLE_ACCURACY);
        } else if (accuracy > 100) {
            qualities.add(Quality.LOW_ACCURACY);
        }
    }

    private static void classifyTime(Instant source, Instant evaluatedAt, EnumSet<Quality> qualities) {
        if (source.isAfter(evaluatedAt)) {
            Duration future = Duration.between(evaluatedAt, source);
            qualities.add(future.compareTo(FUTURE_TOLERANCE) <= 0 ? Quality.CLOCK_SKEW : Quality.FUTURE);
            return;
        }
        Duration age = Duration.between(source, evaluatedAt);
        if (age.compareTo(LATE_THRESHOLD) > 0) {
            qualities.add(Quality.LATE);
        } else if (age.compareTo(DETECTOR_AGE_LIMIT) > 0) {
            qualities.add(Quality.DELAYED);
        }
    }

    private static void classifyBattery(BigDecimal battery, EnumSet<Quality> qualities) {
        if (battery != null && battery.compareTo(BigDecimal.TEN) <= 0) {
            qualities.add(Quality.BATTERY_CRITICAL);
        } else if (battery != null && battery.compareTo(BigDecimal.valueOf(20)) <= 0) {
            qualities.add(Quality.BATTERY_LOW);
        }
    }

    private static Connectivity connectivity(Observation observation, Instant evaluatedAt) {
        Duration receiptAge = Duration.between(observation.receivedAt(), evaluatedAt);
        if (receiptAge.compareTo(OFFLINE_LIMIT) > 0) {
            return Connectivity.OFFLINE;
        }
        Duration sourceAge = Duration.between(observation.sourceTimestamp(), evaluatedAt);
        if (sourceAge.compareTo(OFFLINE_LIMIT) > 0) {
            return Connectivity.STALE;
        }
        if (sourceAge.compareTo(FRESH_LIMIT) > 0 || receiptAge.compareTo(FRESH_LIMIT) > 0) {
            return Connectivity.RECENT;
        }
        return Connectivity.LIVE;
    }

    private static Trust trust(EnumSet<Quality> qualities, boolean recoveryHold) {
        if (recoveryHold || qualities.stream().anyMatch(GpsReliabilityPolicy::isUntrusted)) {
            return Trust.UNTRUSTED;
        }
        if (qualities.contains(Quality.ACCURACY_UNKNOWN)) {
            return Trust.UNKNOWN;
        }
        return Trust.TRUSTED;
    }

    private static boolean isUntrusted(Quality quality) {
        return switch (quality) {
            case LOW_ACCURACY, UNUSABLE_ACCURACY, NULL_ISLAND_SUSPECT, CLOCK_SKEW, DELAYED, LATE,
                    OUT_OF_ORDER, FUTURE, IMPOSSIBLE_MOVEMENT, TAMPER_ACTIVE, BINDING_INVALID -> true;
            default -> false;
        };
    }

    private static ReliabilityState state(
            Trust trust,
            Connectivity connectivity,
            EnumSet<Quality> qualities,
            boolean recoveryHold) {
        if (connectivity == Connectivity.OFFLINE) {
            return ReliabilityState.OFFLINE;
        }
        if (recoveryHold) {
            return ReliabilityState.RECOVERING;
        }
        if (qualities.contains(Quality.IMPOSSIBLE_MOVEMENT) || qualities.contains(Quality.TAMPER_ACTIVE)) {
            return ReliabilityState.SUSPECT;
        }
        if (trust == Trust.UNKNOWN) {
            return ReliabilityState.UNKNOWN;
        }
        if (trust == Trust.UNTRUSTED || connectivity != Connectivity.LIVE
                || !qualities.equals(EnumSet.of(Quality.NORMAL))) {
            return ReliabilityState.DEGRADED;
        }
        return ReliabilityState.NORMAL;
    }
}
