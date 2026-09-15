package com.transportlogistics.app.tracking.application;

import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import static com.transportlogistics.app.tracking.domain.journeyreplay.StopAnalysisPolicy.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class DeterministicJourneyReplayStopAnalyzer {
    private static final double EARTH_RADIUS_METERS = 6_371_008.8d;
    private final UUID tenantId;
    private final UUID vehicleId;
    private final Instant snapshot;
    private final ReplayBoundaryEvidence boundaries;
    private final Coverage coverage;
    private final List<ConfirmedStop> confirmed = new ArrayList<>();
    private final List<List<JourneyPoint>> confirmedEvidence = new ArrayList<>();
    private List<JourneyPoint> candidate = new ArrayList<>();

    DeterministicJourneyReplayStopAnalyzer(UUID tenantId, UUID vehicleId, Instant snapshot,
            ReplayBoundaryEvidence boundaries, Coverage coverage) {
        this.tenantId = tenantId;
        this.vehicleId = vehicleId;
        this.snapshot = snapshot;
        this.boundaries = boundaries;
        this.coverage = coverage;
    }

    void accept(JourneyPoint point) {
        if (!eligible(point)) {
            finalizeCandidate(false, false);
            return;
        }
        if (!candidate.isEmpty() && !gapAllowed(candidate.getLast(), point)) {
            finalizeCandidate(false, false);
        }
        if (candidate.isEmpty()) {
            candidate.add(point);
            return;
        }
        List<JourneyPoint> tentative = new ArrayList<>(candidate);
        tentative.add(point);
        if (withinRadius(tentative)) candidate = tentative;
        else {
            finalizeCandidate(false, false);
            candidate.add(point);
        }
    }

    List<ConfirmedStop> finish() {
        boolean startTruncated = lowerContinues();
        boolean endTruncated = upperContinues(boundaries);
        finalizeCandidate(startTruncated && confirmed.isEmpty(), endTruncated);
        return confirmed.stream().sorted(Comparator.comparing(ConfirmedStop::start)
                .thenComparing(ConfirmedStop::stopId)).toList();
    }

    private boolean lowerContinues() {
        if (candidate.isEmpty()) return false;
        if (coverage == Coverage.PARTIAL_RETENTION) return true;
        JourneyPoint previous = boundaries.lowerBoundary().adjacentPoint();
        return boundaries.lowerBoundary().reason() == BoundaryReason.ADJACENT_QUALIFYING
                && previous != null && gapAllowed(previous, candidate.getFirst())
                && withinRadius(join(List.of(previous), candidate));
    }

    private boolean upperContinues(ReplayBoundaryEvidence boundaries) {
        if (candidate.isEmpty()) return false;
        JourneyPoint next = boundaries.upperBoundary().adjacentPoint();
        return boundaries.upperBoundary().reason() == BoundaryReason.ADJACENT_QUALIFYING
                && next != null && gapAllowed(candidate.getLast(), next)
                && withinRadius(join(candidate, List.of(next)));
    }

    private void finalizeCandidate(boolean startTruncated, boolean endTruncated) {
        if (candidate.isEmpty()) return;
        if (Duration.between(candidate.getFirst().sourceTimestamp(), candidate.getLast().sourceTimestamp())
                .compareTo(MIN_DWELL) >= 0) {
            boolean effectiveStartTruncated = startTruncated || (confirmed.isEmpty() && lowerContinues());
            ConfirmedStop stop = stop(candidate, effectiveStartTruncated, endTruncated);
            if (!confirmed.isEmpty() && mergeable(confirmed.getLast(), stop,
                    distance(confirmed.getLast().centroid(), stop.centroid()))) {
                List<JourneyPoint> combined = join(confirmedEvidence.getLast(), candidate);
                if (withinRadius(combined)) {
                    confirmed.set(confirmed.size() - 1, stop(combined,
                            confirmed.getLast().startTruncated(), endTruncated));
                    confirmedEvidence.set(confirmedEvidence.size() - 1, List.copyOf(combined));
                } else add(stop, candidate);
            } else add(stop, candidate);
        }
        candidate = new ArrayList<>();
    }

    private void add(ConfirmedStop stop, List<JourneyPoint> evidence) {
        confirmed.add(stop);
        confirmedEvidence.add(List.copyOf(evidence));
    }

    private ConfirmedStop stop(List<JourneyPoint> evidence, boolean startTruncated, boolean endTruncated) {
        Coordinate centroid = centroid(evidence);
        BigDecimal radius = evidence.stream().map(point -> distance(centroid, point.coordinate()))
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        boolean missingSpeed = evidence.stream().anyMatch(point -> point.speedKph() == null);
        StopEvidenceQuality quality = startTruncated || endTruncated
                ? StopEvidenceQuality.RANGE_TRUNCATED
                : missingSpeed ? StopEvidenceQuality.SPEED_SPATIAL_ONLY : StopEvidenceQuality.QUALIFIED;
        Set<QualityFlag> flags = EnumSet.noneOf(QualityFlag.class);
        evidence.forEach(point -> flags.addAll(point.qualityFlags()));
        Instant start = evidence.getFirst().sourceTimestamp();
        Instant end = evidence.getLast().sourceTimestamp();
        return new ConfirmedStop(identity(evidence.getFirst().historyId(), evidence.getLast().historyId()),
                centroid, start, end, Duration.between(start, end), evidence.size(), radius,
                startTruncated, endTruncated, quality, Set.copyOf(flags));
    }

    private String identity(UUID first, UUID last) {
        String canonical = "tenant=" + tenantId + "|vehicle=" + vehicleId + "|snapshot=" + snapshot
                + "|start=" + first + "|end=" + last;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    static boolean withinRadius(List<JourneyPoint> points) {
        Coordinate center = centroid(points);
        return points.stream().allMatch(point -> distance(center, point.coordinate())
                .compareTo(MAX_RADIUS_METERS) <= 0);
    }

    static Coordinate centroid(List<JourneyPoint> points) {
        double x = 0d, y = 0d, z = 0d;
        for (JourneyPoint point : points) {
            double accuracy = Math.max(point.accuracyMeters().doubleValue(), 1d);
            double weight = 1d / (accuracy * accuracy);
            double latitude = Math.toRadians(point.coordinate().latitude().doubleValue());
            double longitude = Math.toRadians(point.coordinate().longitude().doubleValue());
            x += weight * Math.cos(latitude) * Math.cos(longitude);
            y += weight * Math.cos(latitude) * Math.sin(longitude);
            z += weight * Math.sin(latitude);
        }
        double longitude = Math.atan2(y, x);
        double latitude = Math.atan2(z, Math.sqrt(x * x + y * y));
        return new Coordinate(BigDecimal.valueOf(Math.toDegrees(latitude)),
                BigDecimal.valueOf(Math.toDegrees(longitude)));
    }

    static BigDecimal distance(Coordinate first, Coordinate second) {
        double lat1 = Math.toRadians(first.latitude().doubleValue());
        double lat2 = Math.toRadians(second.latitude().doubleValue());
        double latitude = lat2 - lat1;
        double longitude = Math.toRadians(second.longitude().subtract(first.longitude()).doubleValue());
        double value = Math.sin(latitude / 2) * Math.sin(latitude / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(longitude / 2) * Math.sin(longitude / 2);
        value = Math.max(0d, Math.min(1d, value));
        return BigDecimal.valueOf(2d * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(value)));
    }

    private static List<JourneyPoint> join(List<JourneyPoint> first, List<JourneyPoint> second) {
        List<JourneyPoint> result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }
}
