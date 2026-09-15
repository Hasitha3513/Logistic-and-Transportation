package com.transportlogistics.app.tracking.adapters.inbound.web.mappers;

import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Gap;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Incident;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Point;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.PointAttribution;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Points;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Range;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Stop;
import com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Stops;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ConfirmedStop;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.DataGap;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentOverlay;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.JourneyPoint;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.TimeRange;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class JourneyReplayWebMapper {
    public Points response(ReplayPage page) {
        return new Points(page.items().stream().map(this::point).toList(), page.nextCursor(),
                page.snapshotRecordedAt(), range(page.requestedRange()), range(page.availableRange()),
                page.coverage(), gaps(page.gaps()), page.truncated(), page.browserCeilingWarning(),
                page.unsupportedEvidence());
    }

    public Stops response(StopPage page) {
        return new Stops(page.items().stream().map(this::stop).toList(), page.nextCursor(),
                page.snapshotRecordedAt(), range(page.requestedRange()), range(page.availableRange()),
                page.coverage(), gaps(page.missingIntervals()), page.analyzedPointCount(), page.pointCeiling());
    }

    public List<Incident> incidents(List<IncidentOverlay> overlays) {
        return overlays.stream().map(value -> new Incident(value.producer(), value.producerAcceptance(),
                value.incidentType(), value.evidenceId(), value.sourceTimestamp(),
                value.endSourceTimestamp(), value.severity(), value.displayLabel(), value.tripId(),
                value.routeId(), value.routeVersion())).toList();
    }

    private Point point(JourneyPoint value) {
        return new Point(value.historyId(), value.vehicleId(), value.sourceTimestamp(), value.receivedAt(),
                coordinate(value.coordinate()), value.speedKph(), value.accuracyMeters(), value.trust(),
                value.quality(), value.ordering(), new PointAttribution(value.attribution().tripId(),
                        value.attribution().routeId(), value.attribution().routeVersion(),
                        value.attribution().status()), value.qualityFlags());
    }

    private Stop stop(ConfirmedStop value) {
        return new Stop(value.stopId(), coordinate(value.centroid()), value.start(), value.end(),
                value.dwell().toSeconds(), value.evidenceCount(), value.radiusMeters(),
                value.startTruncated(), value.endTruncated(), value.quality(), value.qualityFlags());
    }

    private static com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Coordinate coordinate(com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Coordinate value) {
        return new com.transportlogistics.app.tracking.adapters.inbound.web.dto.response.JourneyReplayResponses.Coordinate(value.latitude(), value.longitude());
    }
    private static Range range(TimeRange value) { return value == null ? null : new Range(value.from(), value.to()); }
    private static List<Gap> gaps(List<DataGap> values) {
        return values.stream().map(value -> new Gap(value.from(), value.to(), value.reasons())).toList();
    }
}
