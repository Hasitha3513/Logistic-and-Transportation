package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayError;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Attribution;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionInterval;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionStatus;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ConfirmedStop;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentOverlay;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.JourneyPoint;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.QualityFlag;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.SelectionType;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.TimeRange;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.ReplayQueryPolicy;
import com.transportlogistics.app.tracking.ports.inbound.JourneyReplayQueryUseCase;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayAttributionPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayHistoryPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayRouteContextPort;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class JourneyReplayQueryService implements JourneyReplayQueryUseCase {
    private final JourneyReplayHistoryPort history;
    private final JourneyReplayCursorPort cursors;
    private final JourneyReplayAttributionPort attribution;
    private final JourneyReplayRouteContextPort routes;
    private final Clock clock;

    public JourneyReplayQueryService(JourneyReplayHistoryPort history, JourneyReplayCursorPort cursors,
            JourneyReplayAttributionPort attribution, JourneyReplayRouteContextPort routes, Clock clock) {
        this.history = history;
        this.cursors = cursors;
        this.attribution = attribution;
        this.routes = routes;
        this.clock = clock;
    }

    @Override
    public ReplayPage points(ReplayQuery query) {
        CursorState cursor = query.cursor() == null ? null : cursors.decode(query.cursor());
        if (cursor != null) ReplayQueryPolicy.validateCursor(cursor, query, clock.instant());
        UUID tenant = query.tenant().tenantId();
        UUID vehicle = query.selector().id();
        TimeRange effective = query.effectiveRange();
        if (query.selector().type() == SelectionType.TRIP) {
            var scope = attribution.findReplayScope(tenant, query.selector().id())
                    .orElseThrow(() -> new JourneyReplayException(JourneyReplayError.SAFE_ABSENCE));
            vehicle = scope.vehicleId();
            Instant from = effective.from().isBefore(scope.actualStart())
                    ? scope.actualStart() : effective.from();
            Instant scopeEnd = scope.actualEnd() == null ? effective.to() : scope.actualEnd();
            Instant to = effective.to().isAfter(scopeEnd) ? scopeEnd : effective.to();
            if (!from.isBefore(to)) throw new JourneyReplayException(JourneyReplayError.SAFE_ABSENCE);
            effective = new TimeRange(from, to);
            query = new ReplayQuery(query.tenant(), query.selector(), query.requestedRange(), effective,
                    query.limit(), query.cursor(), query.overlays(), query.direction(),
                    query.browserPointCeiling());
        }
        ReplayPage raw = history.query(query, vehicle, cursor);
        List<AttributionInterval> intervals = attribution.findOverlapping(
                tenant, vehicle, effective.from(), effective.to());
        Map<RouteKey, Boolean> geometry = new HashMap<>();
        List<JourneyPoint> enriched = new ArrayList<>(raw.items().size());
        for (JourneyPoint point : raw.items()) {
            List<AttributionInterval> matches = intervals.stream()
                    .filter(interval -> interval.includes(point.sourceTimestamp())).toList();
            Attribution resolved = resolve(matches);
            Set<QualityFlag> flags = new HashSet<>(point.qualityFlags());
            if (resolved.status() == AttributionStatus.ATTRIBUTED) {
                flags.remove(QualityFlag.ATTRIBUTION_UNKNOWN);
                if (resolved.routeId() != null && resolved.routeVersion() != null) {
                    RouteKey key = new RouteKey(resolved.routeId(), resolved.routeVersion());
                    boolean available = geometry.computeIfAbsent(key,
                            ignored -> routes.findExact(tenant, key.routeId(), key.routeVersion()).isPresent());
                    if (!available) flags.add(QualityFlag.GEOMETRY_UNAVAILABLE);
                }
            }
            enriched.add(withAttribution(point, resolved, flags));
        }
        return new ReplayPage(enriched, raw.nextCursor(), raw.requestedRange(), raw.availableRange(),
                raw.coverage(), raw.gaps(), raw.truncated(), raw.browserCeilingWarning(),
                raw.unsupportedEvidence(), raw.snapshotRecordedAt(), raw.boundaryEvidence());
    }

    private static Attribution resolve(List<AttributionInterval> matches) {
        if (matches.isEmpty()) return Attribution.unavailable(AttributionStatus.UNATTRIBUTED);
        if (matches.size() > 1) return Attribution.unavailable(AttributionStatus.AMBIGUOUS);
        AttributionInterval match = matches.getFirst();
        return new Attribution(match.tripId(), match.routeId(), match.routeVersion(),
                AttributionStatus.ATTRIBUTED);
    }

    private static JourneyPoint withAttribution(
            JourneyPoint point, Attribution value, Set<QualityFlag> flags) {
        return new JourneyPoint(point.historyId(), point.vehicleId(), point.sourceTimestamp(),
                point.receivedAt(), point.coordinate(), point.speedKph(), point.accuracyMeters(),
                point.trust(), point.quality(), point.ordering(), value, flags);
    }

    @Override
    public StopPage stops(StopReplayQuery query) {
        throw new JourneyReplayException(JourneyReplayError.REQUIRED_CAPABILITY_UNAVAILABLE);
    }

    @Override
    public List<IncidentOverlay> incidents(ReplayQuery query) {
        throw new JourneyReplayException(JourneyReplayError.REQUIRED_CAPABILITY_UNAVAILABLE);
    }

    private record RouteKey(UUID routeId, String routeVersion) { }
}
