package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayAttributionPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayHistoryPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayIncidentPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayRouteContextPort;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayStopCursorPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JourneyReplayQueryServiceTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();
    private static final Instant FROM = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant TO = FROM.plusSeconds(600);
    private final JourneyReplayHistoryPort history = mock(JourneyReplayHistoryPort.class);
    private final JourneyReplayCursorPort cursors = mock(JourneyReplayCursorPort.class);
    private final JourneyReplayAttributionPort attribution = mock(JourneyReplayAttributionPort.class);
    private final JourneyReplayRouteContextPort routes = mock(JourneyReplayRouteContextPort.class);
    private final JourneyReplayQueryService service = new JourneyReplayQueryService(
            history, cursors, attribution, routes, Clock.fixed(TO, ZoneOffset.UTC));

    @Test
    void enrichesOnePageWithOneBoundedAssignmentLookupAndExactRouteContext() {
        ReplayQuery query = query(ReplaySelector.vehicle(VEHICLE));
        JourneyPoint point = point(FROM.plusSeconds(10));
        UUID trip = UUID.randomUUID();
        UUID route = UUID.randomUUID();
        ReplayPage raw = new ReplayPage(List.of(point), null, query.requestedRange(),
                query.effectiveRange(), Coverage.COMPLETE, List.of(), false, false, Set.of());
        when(history.query(query, VEHICLE, null)).thenReturn(raw);
        when(attribution.findOverlapping(TENANT, VEHICLE, FROM, TO)).thenReturn(List.of(
                new AttributionInterval(trip, FROM, TO, route, "REVISION:3")));
        when(routes.findExact(TENANT, route, "REVISION:3"))
                .thenReturn(java.util.Optional.of(List.of(new Coordinate(BigDecimal.ONE, BigDecimal.TEN))));

        ReplayPage result = service.points(query);

        assertThat(result.items().getFirst().attribution().tripId()).isEqualTo(trip);
        assertThat(result.items().getFirst().qualityFlags()).doesNotContain(QualityFlag.ATTRIBUTION_UNKNOWN);
        verify(attribution).findOverlapping(TENANT, VEHICLE, FROM, TO);
        verify(routes).findExact(TENANT, route, "REVISION:3");
    }

    @Test
    void preservesAmbiguityInsteadOfGuessing() {
        ReplayQuery query = query(ReplaySelector.vehicle(VEHICLE));
        when(history.query(query, VEHICLE, null)).thenReturn(new ReplayPage(List.of(point(FROM)), null,
                query.requestedRange(), query.effectiveRange(), Coverage.COMPLETE, List.of(), false, false,
                Set.of()));
        when(attribution.findOverlapping(TENANT, VEHICLE, FROM, TO)).thenReturn(List.of(
                new AttributionInterval(UUID.randomUUID(), FROM, TO, null, null),
                new AttributionInterval(UUID.randomUUID(), FROM, TO, null, null)));

        assertThat(service.points(query).items().getFirst().attribution().status())
                .isEqualTo(AttributionStatus.AMBIGUOUS);
    }

    @Test
    void streamsSnapshotStablePagesAndConfirmsStopAcrossPageBoundary() {
        JourneyReplayStopCursorPort stopCursors = mock(JourneyReplayStopCursorPort.class);
        JourneyReplayQueryService stopService = new JourneyReplayQueryService(history, cursors,
                attribution, routes, stopCursors, Clock.fixed(TO, ZoneOffset.UTC));
        ReplayQuery base = query(ReplaySelector.vehicle(VEHICLE));
        Instant snapshot = TO.minusSeconds(1);
        CursorState continuation = new CursorState(new CursorBinding(TENANT, base.selector(),
                base.requestedRange(), snapshot), new CursorPosition(FROM.plusSeconds(240),
                new UUID(0, 3)), snapshot.plusSeconds(900));
        when(cursors.decode("page-2")).thenReturn(continuation);
        when(history.query(any(), eq(VEHICLE), nullable(CursorState.class))).thenAnswer(invocation -> {
            CursorState cursor = invocation.getArgument(2);
            ReplayQuery requested = invocation.getArgument(0);
            List<JourneyPoint> points = cursor == null
                    ? List.of(stopPoint(0, 1), stopPoint(120, 2), stopPoint(240, 3))
                    : List.of(stopPoint(300, 4));
            return new ReplayPage(points, cursor == null ? "page-2" : null,
                    requested.requestedRange(), requested.effectiveRange(), Coverage.COMPLETE,
                    List.of(), false, false, Set.of(), snapshot, ReplayBoundaryEvidence.unavailable());
        });
        when(attribution.findOverlapping(eq(TENANT), eq(VEHICLE), any(), any())).thenReturn(List.of());

        StopPage result = stopService.stops(new StopReplayQuery(base, 100, null));

        assertThat(result.items()).singleElement().satisfies(stop -> {
            assertThat(stop.dwell()).isEqualTo(java.time.Duration.ofMinutes(5));
            assertThat(stop.evidenceCount()).isEqualTo(4);
        });
        assertThat(result.analyzedPointCount()).isEqualTo(4);
        assertThat(result.snapshotRecordedAt()).isEqualTo(snapshot);
        verify(history, times(2)).query(any(), eq(VEHICLE), nullable(CursorState.class));
    }

    @Test
    void delegatesProducerLabelledIncidentsForTheTenantVehicleAndBoundedRange() {
        JourneyReplayStopCursorPort stopCursors = mock(JourneyReplayStopCursorPort.class);
        JourneyReplayIncidentPort incidents = mock(JourneyReplayIncidentPort.class);
        JourneyReplayQueryService incidentService = new JourneyReplayQueryService(history, cursors,
                attribution, routes, stopCursors, incidents, Clock.fixed(TO, ZoneOffset.UTC));
        ReplayQuery query = new ReplayQuery(new TenantContext(TENANT, ACTOR), ReplaySelector.vehicle(VEHICLE),
                new TimeRange(FROM, TO), new TimeRange(FROM, TO), 100, null,
                Set.of(OverlayType.GEOFENCE, OverlayType.SPEED), Direction.CHRONOLOGICAL_ASCENDING, 20_000);
        IncidentOverlay geofence = new IncidentOverlay(OverlayType.GEOFENCE, ProducerAcceptance.ACCEPTED,
                "ENTERED", UUID.randomUUID(), FROM.plusSeconds(10), null, "NORMAL",
                "Geofence transition — ENTERED", null, null, null);
        when(incidents.query(query, null)).thenReturn(List.of(geofence));

        assertThat(incidentService.incidents(query).items()).containsExactly(geofence);
        verify(incidents).query(query, null);
    }

    @Test
    void pagesIncidentEvidenceWithTheExistingTenantBoundOpaqueCursor() {
        JourneyReplayIncidentPort incidents = mock(JourneyReplayIncidentPort.class);
        JourneyReplayQueryService incidentService = new JourneyReplayQueryService(history, cursors,
                attribution, routes, mock(JourneyReplayStopCursorPort.class), incidents,
                Clock.fixed(TO, ZoneOffset.UTC));
        ReplayQuery query = new ReplayQuery(new TenantContext(TENANT, ACTOR), ReplaySelector.vehicle(VEHICLE),
                new TimeRange(FROM, TO), new TimeRange(FROM, TO), 1, null,
                Set.of(OverlayType.GEOFENCE), Direction.CHRONOLOGICAL_ASCENDING, 20_000);
        IncidentOverlay first = new IncidentOverlay(OverlayType.GEOFENCE, ProducerAcceptance.ACCEPTED,
                "ENTERED", UUID.randomUUID(), FROM.plusSeconds(10), null, "NORMAL",
                "Geofence transition — ENTERED", null, null, null);
        IncidentOverlay second = new IncidentOverlay(OverlayType.GEOFENCE, ProducerAcceptance.ACCEPTED,
                "EXITED", UUID.randomUUID(), FROM.plusSeconds(20), null, "NORMAL",
                "Geofence transition — EXITED", null, null, null);
        when(incidents.query(query, null)).thenReturn(List.of(first, second));
        when(cursors.encode(any(CursorState.class))).thenReturn("opaque-next");

        var result = incidentService.incidents(query);

        assertThat(result.items()).containsExactly(first);
        assertThat(result.nextCursor()).isEqualTo("opaque-next");
        assertThat(result.snapshotRecordedAt()).isEqualTo(TO);
    }

    @Test
    void resolvesTripScopeBeforeQueryingIncidentEvidence() {
        JourneyReplayIncidentPort incidents = mock(JourneyReplayIncidentPort.class);
        JourneyReplayQueryService incidentService = new JourneyReplayQueryService(history, cursors,
                attribution, routes, mock(JourneyReplayStopCursorPort.class), incidents,
                Clock.fixed(TO, ZoneOffset.UTC));
        UUID trip = UUID.randomUUID();
        ReplayQuery query = query(ReplaySelector.trip(trip));
        when(attribution.findReplayScope(TENANT, trip)).thenReturn(java.util.Optional.of(
                new ReplayScope(trip, VEHICLE, FROM.plusSeconds(10), TO.minusSeconds(10),
                        "COMPLETED", null, null)));

        incidentService.incidents(query);

        verify(incidents).query(org.mockito.ArgumentMatchers.argThat(resolved ->
                resolved.selector().equals(ReplaySelector.vehicle(VEHICLE))
                        && resolved.effectiveRange().from().equals(FROM.plusSeconds(10))
                        && resolved.effectiveRange().to().equals(TO.minusSeconds(10))), eq(null));
    }

    private static ReplayQuery query(ReplaySelector selector) {
        TimeRange range = new TimeRange(FROM, TO);
        return new ReplayQuery(new TenantContext(TENANT, ACTOR), selector, range, range, 1000,
                null, Set.of(), Direction.CHRONOLOGICAL_ASCENDING, 20_000);
    }

    private static JourneyPoint point(Instant source) {
        return new JourneyPoint(UUID.randomUUID(), VEHICLE, source, source.plusSeconds(1),
                new Coordinate(BigDecimal.ONE, BigDecimal.TEN), null, null, Trust.TRUSTED,
                "ACCURACY_UNKNOWN", Ordering.IN_ORDER,
                Attribution.unavailable(AttributionStatus.UNATTRIBUTED),
                Set.of(QualityFlag.ATTRIBUTION_UNKNOWN));
    }

    private static JourneyPoint stopPoint(int seconds, long identity) {
        Instant source = FROM.plusSeconds(seconds);
        return new JourneyPoint(new UUID(0, identity), VEHICLE, source, source.plusSeconds(1),
                new Coordinate(BigDecimal.ONE, BigDecimal.TEN), BigDecimal.ZERO, BigDecimal.ONE,
                Trust.TRUSTED, "GOOD", Ordering.IN_ORDER,
                Attribution.unavailable(AttributionStatus.UNATTRIBUTED), Set.of());
    }
}
