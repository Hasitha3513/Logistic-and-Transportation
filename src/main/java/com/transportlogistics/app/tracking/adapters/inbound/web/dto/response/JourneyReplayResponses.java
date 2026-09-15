package com.transportlogistics.app.tracking.adapters.inbound.web.dto.response;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionStatus;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coverage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Ordering;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.OverlayType;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ProducerAcceptance;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.QualityFlag;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopEvidenceQuality;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Trust;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class JourneyReplayResponses {
    private JourneyReplayResponses() { }

    public record Range(Instant from, Instant to) { }
    public record Coordinate(BigDecimal latitude, BigDecimal longitude) { }
    public record PointAttribution(UUID tripId, UUID routeId, String routeVersion,
                                   AttributionStatus status) { }
    public record Gap(Instant from, Instant to, Set<QualityFlag> reasons) { }
    public record Point(UUID historyId, UUID vehicleId, Instant sourceTimestamp, Instant receivedAt,
                        Coordinate coordinate, BigDecimal speedKph, BigDecimal accuracyMeters,
                        Trust trust, String quality, Ordering ordering, PointAttribution attribution,
                        Set<QualityFlag> qualityFlags) { }
    public record Points(List<Point> items, String nextCursor, Instant snapshotRecordedAt,
                         Range requestedRange, Range availableRange, Coverage coverage,
                         List<Gap> missingIntervals, boolean truncated,
                         boolean browserCeilingWarning, Set<String> unsupportedEvidence) { }
    public record Stop(String stopId, Coordinate centroid, Instant start, Instant end,
                       long durationSeconds, int pointCount, BigDecimal radiusMeters,
                       boolean startTruncated, boolean endTruncated,
                       StopEvidenceQuality quality, Set<QualityFlag> qualityFlags) { }
    public record Stops(List<Stop> items, String nextCursor, Instant snapshotRecordedAt,
                        Range requestedRange, Range availableRange, Coverage coverage,
                        List<Gap> missingIntervals, int analyzedPointCount, int pointCeiling) { }
    public record Incident(OverlayType producer, ProducerAcceptance evidenceStatus,
                           String incidentType, UUID evidenceId, Instant sourceTimestamp,
                           Instant endSourceTimestamp, String severity, String status,
                           UUID tripId, UUID routeId, String routeVersion) { }
    public record Incidents(List<Incident> items, String nextCursor, Instant snapshotRecordedAt,
                            Range requestedRange, Range availableRange, Coverage coverage,
                            List<Gap> missingIntervals, Set<OverlayType> producerStatus) { }
}
