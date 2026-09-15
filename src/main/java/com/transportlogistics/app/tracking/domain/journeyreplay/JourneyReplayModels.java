package com.transportlogistics.app.tracking.domain.journeyreplay;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class JourneyReplayModels {
    public static final int DEFAULT_POINT_LIMIT = 1_000;
    public static final int MAX_POINT_LIMIT = 2_000;
    public static final int BROWSER_POINT_CEILING = 20_000;
    public static final int DEFAULT_STOP_LIMIT = 100;
    public static final int MAX_STOP_LIMIT = 500;
    public static final String STOP_RULE_VERSION = "US53_STOP_V1";

    private JourneyReplayModels() { }

    public enum SelectionType { VEHICLE, TRIP }
    public enum Direction { CHRONOLOGICAL_ASCENDING }
    public enum Coverage { COMPLETE, PARTIAL_RETENTION, NO_DATA }
    public enum AttributionStatus { ATTRIBUTED, UNATTRIBUTED, AMBIGUOUS }
    public enum Trust { TRUSTED, UNTRUSTED, UNKNOWN }
    public enum Ordering { IN_ORDER, CLOCK_SKEW, OUT_OF_ORDER, LATE, FUTURE }
    public enum OverlayType { GEOFENCE, SPEED, ROUTE_DEVIATION, IDLE }
    public enum ProducerAcceptance {
        ACCEPTED, FIELD_FIDELITY_PENDING, FIELD_ACCEPTANCE_PENDING, UNAVAILABLE
    }
    public enum QualityFlag {
        UNTRUSTED, ACCURACY_UNKNOWN, ACCURACY_LOW, STALE_AT_RECEIPT, TIME_GAP,
        LARGE_JUMP, OUT_OF_ORDER, LATE, CLOCK_SKEW, ATTRIBUTION_UNKNOWN,
        GEOMETRY_UNAVAILABLE, PARTIAL_RETENTION
    }
    public enum StopEvidenceQuality { QUALIFIED, SPEED_SPATIAL_ONLY, RANGE_TRUNCATED }
    public enum BoundaryReason {
        NO_ADJACENT_EVIDENCE, ADJACENT_INELIGIBLE, ADJACENT_QUALIFYING,
        RETENTION_UNAVAILABLE, SNAPSHOT_UNAVAILABLE, REQUESTED_RANGE_ENDED, NOT_SAFE_TO_EVALUATE
    }

    public record TenantContext(UUID tenantId, UUID actorId) {
        public TenantContext {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(actorId, "actorId");
        }
    }

    public record ReplaySelector(SelectionType type, UUID id) {
        public ReplaySelector {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(id, "id");
        }

        public static ReplaySelector vehicle(UUID id) { return new ReplaySelector(SelectionType.VEHICLE, id); }
        public static ReplaySelector trip(UUID id) { return new ReplaySelector(SelectionType.TRIP, id); }
    }

    public record TimeRange(Instant from, Instant to) {
        public TimeRange {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }

        public Duration duration() { return Duration.between(from, to); }
    }

    public record ReplayRequest(UUID vehicleId, UUID tripId, Instant from, Instant to,
                                Integer limit, String cursor, Set<OverlayType> overlays,
                                boolean exportRequested) {
        public ReplayRequest {
            overlays = overlays == null ? Set.of() : Set.copyOf(overlays);
        }
    }

    public record ReplayQuery(TenantContext tenant, ReplaySelector selector, TimeRange requestedRange,
                              TimeRange effectiveRange, int limit, String cursor,
                              Set<OverlayType> overlays, Direction direction, int browserPointCeiling) {
        public ReplayQuery {
            Objects.requireNonNull(tenant, "tenant");
            Objects.requireNonNull(selector, "selector");
            Objects.requireNonNull(requestedRange, "requestedRange");
            Objects.requireNonNull(effectiveRange, "effectiveRange");
            overlays = Set.copyOf(overlays);
            Objects.requireNonNull(direction, "direction");
        }
    }

    public record Coordinate(BigDecimal latitude, BigDecimal longitude) {
        public Coordinate {
            Objects.requireNonNull(latitude, "latitude");
            Objects.requireNonNull(longitude, "longitude");
            if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                    || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                    || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                    || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new JourneyReplayException(JourneyReplayError.INVALID_COORDINATE);
            }
        }
    }

    public record Attribution(UUID tripId, UUID routeId, String routeVersion,
                              AttributionStatus status) {
        public Attribution {
            Objects.requireNonNull(status, "status");
        }

        public static Attribution unavailable(AttributionStatus status) {
            return new Attribution(null, null, null, status);
        }
    }

    public record ReplayScope(UUID tripId, UUID vehicleId, Instant actualStart, Instant actualEnd,
                              String status, UUID routeId, String routeVersion) { }

    public record AttributionInterval(UUID tripId, Instant effectiveStart, Instant effectiveEnd,
                                      UUID routeId, String routeVersion) {
        public boolean includes(Instant sourceTimestamp) {
            return !sourceTimestamp.isBefore(effectiveStart)
                    && (effectiveEnd == null || sourceTimestamp.isBefore(effectiveEnd));
        }
    }

    public record JourneyPoint(UUID historyId, UUID vehicleId, Instant sourceTimestamp,
                               Instant receivedAt, Coordinate coordinate, BigDecimal speedKph,
                               BigDecimal accuracyMeters, Trust trust, String quality,
                               Ordering ordering, Attribution attribution,
                               Set<QualityFlag> qualityFlags) implements Comparable<JourneyPoint> {
        public JourneyPoint {
            Objects.requireNonNull(historyId, "historyId");
            Objects.requireNonNull(vehicleId, "vehicleId");
            Objects.requireNonNull(sourceTimestamp, "sourceTimestamp");
            Objects.requireNonNull(receivedAt, "receivedAt");
            Objects.requireNonNull(coordinate, "coordinate");
            Objects.requireNonNull(trust, "trust");
            Objects.requireNonNull(quality, "quality");
            Objects.requireNonNull(ordering, "ordering");
            Objects.requireNonNull(attribution, "attribution");
            qualityFlags = qualityFlags == null ? Set.of() : Set.copyOf(qualityFlags);
        }

        @Override public int compareTo(JourneyPoint other) {
            int timestamp = sourceTimestamp.compareTo(other.sourceTimestamp);
            return timestamp != 0 ? timestamp : historyId.compareTo(other.historyId);
        }
    }

    public record DataGap(Instant from, Instant to, Set<QualityFlag> reasons) {
        public DataGap {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            reasons = Set.copyOf(reasons);
        }
    }

    public record BoundaryObservation(BoundaryReason reason, JourneyPoint adjacentPoint) {
        public BoundaryObservation { Objects.requireNonNull(reason, "reason"); }
    }

    public record ReplayBoundaryEvidence(BoundaryObservation lowerBoundary,
                                         BoundaryObservation upperBoundary) {
        public ReplayBoundaryEvidence {
            Objects.requireNonNull(lowerBoundary, "lowerBoundary");
            Objects.requireNonNull(upperBoundary, "upperBoundary");
        }
        public static ReplayBoundaryEvidence unavailable() {
            return new ReplayBoundaryEvidence(
                    new BoundaryObservation(BoundaryReason.NOT_SAFE_TO_EVALUATE, null),
                    new BoundaryObservation(BoundaryReason.NOT_SAFE_TO_EVALUATE, null));
        }
    }

    public record ReplayPage(List<JourneyPoint> items, String nextCursor, TimeRange requestedRange,
                             TimeRange availableRange, Coverage coverage, List<DataGap> gaps,
                             boolean truncated, boolean browserCeilingWarning,
                             Set<String> unsupportedEvidence, Instant snapshotRecordedAt,
                             ReplayBoundaryEvidence boundaryEvidence) {
        public ReplayPage {
            items = List.copyOf(items);
            Objects.requireNonNull(requestedRange, "requestedRange");
            Objects.requireNonNull(coverage, "coverage");
            gaps = List.copyOf(gaps);
            unsupportedEvidence = Set.copyOf(unsupportedEvidence);
            Objects.requireNonNull(snapshotRecordedAt, "snapshotRecordedAt");
            Objects.requireNonNull(boundaryEvidence, "boundaryEvidence");
            if (coverage == Coverage.PARTIAL_RETENTION && availableRange == null) {
                throw new JourneyReplayException(JourneyReplayError.RETENTION_UNAVAILABLE);
            }
            if (coverage == Coverage.NO_DATA && !items.isEmpty()) {
                throw new JourneyReplayException(JourneyReplayError.INVALID_COVERAGE);
            }
        }

        public ReplayPage(List<JourneyPoint> items, String nextCursor, TimeRange requestedRange,
                TimeRange availableRange, Coverage coverage, List<DataGap> gaps, boolean truncated,
                boolean browserCeilingWarning, Set<String> unsupportedEvidence) {
            this(items, nextCursor, requestedRange, availableRange, coverage, gaps, truncated,
                    browserCeilingWarning, unsupportedEvidence, Instant.EPOCH,
                    ReplayBoundaryEvidence.unavailable());
        }
    }

    public record CursorBinding(UUID tenantId, ReplaySelector selector, TimeRange requestedRange,
                                Instant snapshotRecordedAt) { }

    public record CursorPosition(Instant sourceTimestamp, UUID historyId) { }

    public record CursorState(CursorBinding binding, CursorPosition position, Instant expiresAt) { }

    public record StopReplayQuery(ReplayQuery replayQuery, int stopLimit, String stopCursor) {
        public StopReplayQuery {
            Objects.requireNonNull(replayQuery, "replayQuery");
            if (stopLimit < 1 || stopLimit > MAX_STOP_LIMIT) {
                throw new JourneyReplayException(JourneyReplayError.INVALID_PAGE_SIZE);
            }
            if (stopCursor != null && stopCursor.isBlank()) {
                throw new JourneyReplayException(JourneyReplayError.STOP_CURSOR_INVALID);
            }
        }
        public static StopReplayQuery defaults(ReplayQuery query) {
            return new StopReplayQuery(query, DEFAULT_STOP_LIMIT, null);
        }
    }

    public record StopCursorState(UUID tenantId, ReplaySelector selector, TimeRange requestedRange,
                                  TimeRange effectiveRange, Instant snapshotRecordedAt,
                                  Instant lastStartSourceTimestamp, String lastStopId,
                                  String ruleVersion, Instant expiresAt) { }

    public record StopPage(List<ConfirmedStop> items, String nextCursor, Instant snapshotRecordedAt,
                           TimeRange requestedRange, TimeRange availableRange, Coverage coverage,
                           List<DataGap> missingIntervals, int analyzedPointCount, int pointCeiling) {
        public StopPage {
            items = List.copyOf(items);
            Objects.requireNonNull(snapshotRecordedAt, "snapshotRecordedAt");
            Objects.requireNonNull(requestedRange, "requestedRange");
            Objects.requireNonNull(coverage, "coverage");
            missingIntervals = List.copyOf(missingIntervals);
            if (analyzedPointCount < 0 || analyzedPointCount > BROWSER_POINT_CEILING
                    || pointCeiling != BROWSER_POINT_CEILING) {
                throw new JourneyReplayException(JourneyReplayError.REPLAY_POINT_LIMIT_EXCEEDED);
            }
        }
    }

    public record StopCandidate(List<JourneyPoint> evidence, Coordinate centroid,
                                Instant start, Instant end, boolean startTruncated,
                                boolean endTruncated, StopEvidenceQuality quality) {
        public StopCandidate {
            evidence = List.copyOf(evidence);
            if (evidence.isEmpty()) throw new JourneyReplayException(JourneyReplayError.INSUFFICIENT_EVIDENCE);
            Objects.requireNonNull(centroid, "centroid");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            Objects.requireNonNull(quality, "quality");
        }

        public Duration dwell() { return Duration.between(start, end); }
    }

    public record StopAnalysisInput(List<JourneyPoint> points, TimeRange requestedRange,
                                    Coverage coverage, boolean startTruncated,
                                    boolean endTruncated) {
        public StopAnalysisInput {
            points = List.copyOf(points);
            Objects.requireNonNull(requestedRange, "requestedRange");
            Objects.requireNonNull(coverage, "coverage");
        }
    }

    public record ConfirmedStop(String stopId, Coordinate centroid, Instant start, Instant end,
                                Duration dwell, int evidenceCount, BigDecimal radiusMeters,
                                boolean startTruncated, boolean endTruncated,
                                StopEvidenceQuality quality, Set<QualityFlag> qualityFlags) { }

    public record IncidentOverlay(OverlayType producer, ProducerAcceptance producerAcceptance,
                                  String incidentType, UUID evidenceId, Instant sourceTimestamp,
                                  Instant endSourceTimestamp, String severity, String displayLabel,
                                  UUID tripId, UUID routeId, String routeVersion) {
        public IncidentOverlay {
            Objects.requireNonNull(producer, "producer");
            Objects.requireNonNull(producerAcceptance, "producerAcceptance");
            Objects.requireNonNull(incidentType, "incidentType");
            Objects.requireNonNull(evidenceId, "evidenceId");
            Objects.requireNonNull(sourceTimestamp, "sourceTimestamp");
            Objects.requireNonNull(displayLabel, "displayLabel");
        }
    }

    public record IncidentPage(List<IncidentOverlay> items, String nextCursor,
                               Instant snapshotRecordedAt) {
        public IncidentPage {
            items = List.copyOf(items);
            Objects.requireNonNull(snapshotRecordedAt, "snapshotRecordedAt");
        }
    }
}
