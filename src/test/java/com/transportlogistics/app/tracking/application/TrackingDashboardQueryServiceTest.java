package com.transportlogistics.app.tracking.application;

import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Connectivity.CONNECTED;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Freshness.LIVE;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.IncidentType.GEOFENCE;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.IncidentType.SPEED;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.ProducerStatus.ACCEPTED;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.SourceStatus.AVAILABLE;
import static com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Trust.TRUSTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Disclosure;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Incident;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.LiveVehicleState;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Observation;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.TripContext;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardCursorPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardIncidentPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardLiveStatePort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardTripContextPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrackingDashboardQueryServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-16T04:00:00Z");

    @Test
    void aggregatesBoundedSourcesAndPreservesProducerAcceptanceLabels() {
        UUID tenantId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        var observation = new Observation(NOW.minusSeconds(10), NOW.minusSeconds(5), TRUSTED,
                new BigDecimal("6.927"), new BigDecimal("79.861"), BigDecimal.TEN,
                new BigDecimal("12.5"));
        var state = new LiveVehicleState(vehicleId, observation, observation, LIVE, CONNECTED,
                "US54-DASHBOARD-V1", NOW);
        var live = mock(TrackingDashboardLiveStatePort.class);
        when(live.find(eq(tenantId), any(), eq(null), eq(50), eq(NOW)))
                .thenReturn(new TrackingDashboardLiveStatePort.LiveStatePage(
                        List.of(state), 1, false, AVAILABLE, NOW.minusSeconds(5)));
        var incidentPort = mock(TrackingDashboardIncidentPort.class);
        var incident = new Incident(UUID.randomUUID(), GEOFENCE, vehicleId, null, null, null,
                "HIGH", "UNAUTHORIZED_ZONE_ENTERED", NOW.minusSeconds(20), ACCEPTED);
        when(incidentPort.find(eq(tenantId), eq(Set.of(vehicleId)), eq(Set.of(GEOFENCE)),
                any(), eq(NOW), anyInt(), anyInt()))
                .thenReturn(new TrackingDashboardIncidentPort.IncidentResult(
                        List.of(incident), Map.of(GEOFENCE, AVAILABLE)));
        var trips = mock(TrackingDashboardTripContextPort.class);
        when(trips.findActiveContexts(tenantId, Set.of(vehicleId), NOW))
                .thenReturn(List.of(new TripContext(vehicleId, tripId, "DISPATCHED", null, null)));
        var cursors = mock(TrackingDashboardCursorPort.class);
        var service = new TrackingDashboardQueryService(live, incidentPort, trips, cursors,
                Clock.fixed(NOW, ZoneOffset.UTC));

        var page = service.query(new DashboardQuery(tenantId,
                        new DashboardFilter(Set.of(), Set.of(), Set.of(), Set.of(),
                                Set.of(GEOFENCE, SPEED), true, true), null, 50),
                new Disclosure(true, true, false, false, true));

        assertThat(page.vehicles()).hasSize(1);
        assertThat(page.vehicles().getFirst().tripContext().tripId()).isEqualTo(tripId);
        assertThat(page.vehicles().getFirst().incidentCounts()).containsEntry(GEOFENCE, 1);
        assertThat(page.heatMapCells()).singleElement().satisfies(cell -> {
            assertThat(cell.latitude()).isEqualByComparingTo("6.925");
            assertThat(cell.longitude()).isEqualByComparingTo("79.865");
        });
        assertThat(page.producerStatuses()).containsEntry("US-49", ACCEPTED);
        assertThat(page.producerStatuses().get("US-51").name()).isEqualTo("UNAVAILABLE");
        assertThat(page.summary().matchingVehicleCount()).isEqualTo(1);
        verify(trips).findActiveContexts(tenantId, Set.of(vehicleId), NOW);
    }

    @Test
    void omitsUnauthorizedCoordinatesIncidentsAndReplayNavigation() {
        UUID tenantId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        var observation = new Observation(NOW, NOW, TRUSTED, BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.TEN);
        var live = mock(TrackingDashboardLiveStatePort.class);
        when(live.find(eq(tenantId), any(), eq(null), eq(10), eq(NOW)))
                .thenReturn(new TrackingDashboardLiveStatePort.LiveStatePage(List.of(
                        new LiveVehicleState(vehicleId, observation, observation, LIVE, CONNECTED,
                                "v1", NOW)), 1, false, AVAILABLE, NOW));
        var incidents = mock(TrackingDashboardIncidentPort.class);
        var trips = mock(TrackingDashboardTripContextPort.class);
        when(trips.findActiveContexts(tenantId, Set.of(vehicleId), NOW)).thenReturn(List.of());
        var service = new TrackingDashboardQueryService(live, incidents, trips,
                mock(TrackingDashboardCursorPort.class), Clock.fixed(NOW, ZoneOffset.UTC));

        var page = service.query(new DashboardQuery(tenantId,
                        new DashboardFilter(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), true, true),
                        null, 10), new Disclosure(false, false, false, false, false));

        assertThat(page.incidents()).isEmpty();
        assertThat(page.heatMapCells()).isEmpty();
        assertThat(page.vehicles().getFirst().latestTrusted().latitude()).isNull();
        assertThat(page.vehicles().getFirst().latestTrusted().accuracyMeters()).isNull();
        assertThat(page.vehicles().getFirst().journeyReplayAvailable()).isFalse();
    }
}
