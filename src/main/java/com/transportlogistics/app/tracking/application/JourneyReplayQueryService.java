package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayError;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayException;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Attribution;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionInterval;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionStatus;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ConfirmedStop;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorBinding;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorPosition;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentOverlay;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.JourneyPoint;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.QualityFlag;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplaySelector;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.SelectionType;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.TimeRange;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopPage;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.StopCursorState;
import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.BROWSER_POINT_CEILING;
import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.MAX_POINT_LIMIT;
import static com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.STOP_RULE_VERSION;
import com.transportlogistics.app.tracking.domain.journeyreplay.ReplayQueryPolicy;
import com.transportlogistics.app.tracking.domain.journeyreplay.ReplayStreamGuard;
import com.transportlogistics.app.tracking.ports.inbound.JourneyReplayQueryUseCase;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayAttributionPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayHistoryPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayIncidentPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayRouteContextPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayStopCursorPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.UUID;

public final class JourneyReplayQueryService implements JourneyReplayQueryUseCase {
    private final JourneyReplayHistoryPort history;
    private final JourneyReplayCursorPort cursors;
    private final JourneyReplayAttributionPort attribution;
    private final JourneyReplayRouteContextPort routes;
    private final JourneyReplayStopCursorPort stopCursors;
    private final JourneyReplayIncidentPort incidents;
    private final Clock clock;

    public JourneyReplayQueryService(JourneyReplayHistoryPort history, JourneyReplayCursorPort cursors,
            JourneyReplayAttributionPort attribution, JourneyReplayRouteContextPort routes, Clock clock) {
        this(history, cursors, attribution, routes, null, clock);
    }

    public JourneyReplayQueryService(JourneyReplayHistoryPort history, JourneyReplayCursorPort cursors,
            JourneyReplayAttributionPort attribution, JourneyReplayRouteContextPort routes,
            JourneyReplayStopCursorPort stopCursors, Clock clock) {
        this.history = history;
        this.cursors = cursors;
        this.attribution = attribution;
        this.routes = routes;
        this.stopCursors = stopCursors;
        this.incidents = null;
        this.clock = clock;
    }

    public JourneyReplayQueryService(JourneyReplayHistoryPort history, JourneyReplayCursorPort cursors,
            JourneyReplayAttributionPort attribution, JourneyReplayRouteContextPort routes,
            JourneyReplayStopCursorPort stopCursors, JourneyReplayIncidentPort incidents, Clock clock) {
        this.history = history;
        this.cursors = cursors;
        this.attribution = attribution;
        this.routes = routes;
        this.stopCursors = stopCursors;
        this.incidents = incidents;
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
        if (stopCursors == null) throw new JourneyReplayException(JourneyReplayError.REQUIRED_CAPABILITY_UNAVAILABLE);
        ReplayQuery base = query.replayQuery();
        StopCursorState stopCursor = query.stopCursor() == null ? null : stopCursors.decode(query.stopCursor());
        if (stopCursor != null) ReplayQueryPolicy.validateStopCursor(stopCursor, query, clock.instant());
        String pointCursor = stopCursor == null ? null : cursors.encode(new CursorState(
                new CursorBinding(base.tenant().tenantId(), base.selector(), base.requestedRange(),
                        stopCursor.snapshotRecordedAt()),
                new CursorPosition(base.effectiveRange().from().minusNanos(1), new UUID(0, 0)),
                stopCursor.expiresAt()));
        ReplayStreamGuard guard = new ReplayStreamGuard();
        ReplayPage first = null;
        DeterministicJourneyReplayStopAnalyzer analyzer = null;
        UUID vehicle = resolveVehicle(base);
        while (true) {
            ReplayQuery pageQuery = new ReplayQuery(base.tenant(), base.selector(), base.requestedRange(),
                    base.effectiveRange(), MAX_POINT_LIMIT, pointCursor, base.overlays(), base.direction(),
                    base.browserPointCeiling());
            ReplayPage page = points(pageQuery);
            guard.accept(pointCursor, page);
            if (first == null) {
                first = page;
                analyzer = new DeterministicJourneyReplayStopAnalyzer(base.tenant().tenantId(), vehicle,
                        page.snapshotRecordedAt(), page.boundaryEvidence(), page.coverage());
            }
            for (JourneyPoint point : page.items()) analyzer.accept(point);
            if (guard.count() == BROWSER_POINT_CEILING && page.nextCursor() != null) {
                throw new JourneyReplayException(JourneyReplayError.REPLAY_POINT_LIMIT_EXCEEDED);
            }
            if (page.nextCursor() == null) break;
            pointCursor = page.nextCursor();
        }
        List<ConfirmedStop> all = analyzer.finish();
        if (stopCursor != null) all = all.stream().filter(stop -> after(stop, stopCursor)).toList();
        all = all.stream().sorted(Comparator.comparing(ConfirmedStop::start)
                .thenComparing(ConfirmedStop::stopId)).toList();
        boolean more = all.size() > query.stopLimit();
        List<ConfirmedStop> items = more ? List.copyOf(all.subList(0, query.stopLimit())) : List.copyOf(all);
        String next = more ? encodeStopCursor(base, first.snapshotRecordedAt(), items.getLast()) : null;
        return new StopPage(items, next, first.snapshotRecordedAt(), first.requestedRange(),
                first.availableRange(), first.coverage(), first.gaps(), guard.count(), BROWSER_POINT_CEILING);
    }

    private UUID resolveVehicle(ReplayQuery query) {
        if (query.selector().type() == SelectionType.VEHICLE) return query.selector().id();
        return attribution.findReplayScope(query.tenant().tenantId(), query.selector().id())
                .orElseThrow(() -> new JourneyReplayException(JourneyReplayError.SAFE_ABSENCE)).vehicleId();
    }

    private String encodeStopCursor(ReplayQuery query, Instant snapshot, ConfirmedStop last) {
        return stopCursors.encode(new StopCursorState(query.tenant().tenantId(), query.selector(),
                query.requestedRange(), query.effectiveRange(), snapshot, last.start(), last.stopId(),
                STOP_RULE_VERSION, snapshot.plus(Duration.ofMinutes(15))));
    }

    private static boolean after(ConfirmedStop stop, StopCursorState cursor) {
        int time = stop.start().compareTo(cursor.lastStartSourceTimestamp());
        return time > 0 || time == 0 && stop.stopId().compareTo(cursor.lastStopId()) > 0;
    }

    @Override
    public IncidentPage incidents(ReplayQuery query) {
        if (incidents == null) throw new JourneyReplayException(JourneyReplayError.REQUIRED_CAPABILITY_UNAVAILABLE);
        ReplayQuery resolved = query;
        if (query.selector().type() == SelectionType.TRIP) {
            var scope = attribution.findReplayScope(query.tenant().tenantId(), query.selector().id())
                    .orElseThrow(() -> new JourneyReplayException(JourneyReplayError.SAFE_ABSENCE));
            Instant from = query.effectiveRange().from().isBefore(scope.actualStart())
                    ? scope.actualStart() : query.effectiveRange().from();
            Instant scopeEnd = scope.actualEnd() == null ? query.effectiveRange().to() : scope.actualEnd();
            Instant to = query.effectiveRange().to().isAfter(scopeEnd) ? scopeEnd : query.effectiveRange().to();
            if (!from.isBefore(to)) throw new JourneyReplayException(JourneyReplayError.SAFE_ABSENCE);
            resolved = new ReplayQuery(query.tenant(), ReplaySelector.vehicle(scope.vehicleId()),
                    query.requestedRange(), new TimeRange(from, to), query.limit(), query.cursor(),
                    query.overlays(), query.direction(), query.browserPointCeiling());
        }
        CursorState cursor = query.cursor() == null ? null : cursors.decode(query.cursor());
        if (cursor != null) ReplayQueryPolicy.validateCursor(cursor, query, clock.instant());
        List<IncidentOverlay> candidates = incidents.query(resolved, cursor);
        boolean more = candidates.size() > query.limit();
        List<IncidentOverlay> items = more
                ? List.copyOf(candidates.subList(0, query.limit())) : List.copyOf(candidates);
        Instant snapshot = cursor == null ? clock.instant() : cursor.binding().snapshotRecordedAt();
        String next = more ? encodeIncidentCursor(query, snapshot, items.getLast()) : null;
        return new IncidentPage(items, next, snapshot);
    }

    private String encodeIncidentCursor(ReplayQuery query, Instant snapshot, IncidentOverlay last) {
        return cursors.encode(new CursorState(new CursorBinding(query.tenant().tenantId(),
                query.selector(), query.requestedRange(), snapshot),
                new CursorPosition(last.sourceTimestamp(), last.evidenceId()),
                snapshot.plus(Duration.ofMinutes(15))));
    }

    private record RouteKey(UUID routeId, String routeVersion) { }
}
