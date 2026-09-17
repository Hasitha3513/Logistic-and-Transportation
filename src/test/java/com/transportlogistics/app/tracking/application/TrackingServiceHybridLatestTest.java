package com.transportlogistics.app.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.fleet.FleetReportingQuery;
import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.domain.TrackingModels;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackingServiceHybridLatestTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID VEHICLE = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-17T08:00:00Z");
    private final TrackingStore store = mock(TrackingStore.class);
    private final LiveTelemetryProjectionPort live = mock(LiveTelemetryProjectionPort.class);
    private TrackingService service;

    @BeforeEach
    void setUp() {
        service = new TrackingService(store, mock(FleetReportingQuery.class),
                Clock.fixed(NOW, ZoneOffset.UTC), live);
    }

    @Test
    void newerEligibleRedisProjectionWinsOverStalePostgreSqlState() {
        when(store.state(TENANT, VEHICLE, NOW)).thenReturn(Optional.of(
                stored(Instant.parse("2026-09-17T07:58:00Z"))));
        when(live.find(TENANT, VEHICLE)).thenReturn(Optional.of(
                projected(Instant.parse("2026-09-17T07:59:50Z"))));

        var result = service.latest(TENANT, VEHICLE, NOW);

        assertThat(result.latestTrusted().sourceTimestamp())
                .isEqualTo(Instant.parse("2026-09-17T07:59:50Z"));
        verify(live).find(TENANT, VEHICLE);
    }

    @Test
    void olderRedisProjectionCannotRegressPostgreSqlState() {
        when(store.state(TENANT, VEHICLE, NOW)).thenReturn(Optional.of(
                stored(Instant.parse("2026-09-17T07:59:50Z"))));
        when(live.find(TENANT, VEHICLE)).thenReturn(Optional.of(
                projected(Instant.parse("2026-09-17T07:58:00Z"))));

        assertThat(service.latest(TENANT, VEHICLE, NOW).latestTrusted().sourceTimestamp())
                .isEqualTo(Instant.parse("2026-09-17T07:59:50Z"));
    }

    @Test
    void RedisFailureIsNotMisreportedAsMissingData() {
        when(store.state(TENANT, VEHICLE, NOW)).thenReturn(Optional.empty());
        when(live.find(TENANT, VEHICLE)).thenThrow(new DependencyUnavailableException(
                "TRACKING_LIVE_PROJECTION_UNAVAILABLE", "Live telemetry projection is unavailable", null));

        assertThatThrownBy(() -> service.latest(TENANT, VEHICLE, NOW))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessage("Live telemetry projection is unavailable");
    }

    private static TrackingModels.State stored(Instant sourceTime) {
        var observation = observation(UUID.randomUUID(), sourceTime);
        return new TrackingModels.State(VEHICLE, observation, observation,
                TrackingModels.Freshness.LIVE, TrackingModels.Connectivity.CONNECTED, "V87", NOW);
    }

    private static TrackingModels.Observation observation(UUID id, Instant sourceTime) {
        return new TrackingModels.Observation(id, TENANT, UUID.randomUUID(), VEHICLE, "FIXTURE", "message",
                null, id.toString(), null, sourceTime, sourceTime.plusSeconds(1),
                new BigDecimal("6.9271"), new BigDecimal("79.8612"), new BigDecimal("5"),
                new BigDecimal("40"), null, null, TrackingModels.EngineState.UNKNOWN, null, null,
                TrackingModels.Trust.TRUSTED, "ACCEPTABLE", TrackingModels.Ordering.IN_ORDER,
                "TIMESCALE_RAW_180_DAYS", "V87", null, Map.of());
    }

    private static LiveTelemetryProjection projected(Instant sourceTime) {
        UUID eventId = UUID.randomUUID();
        var event = new TrackingTelemetryIngestedV2(eventId, TrackingTelemetryIngestedV2.TYPE,
                TrackingTelemetryIngestedV2.VERSION, TENANT, VEHICLE, UUID.randomUUID(), "FIXTURE", "message",
                eventId.toString(), new BigDecimal("6.9271"), new BigDecimal("79.8612"),
                new BigDecimal("40"), null, new BigDecimal("5"), null,
                TrackingModels.EngineState.UNKNOWN, null, null, sourceTime, sourceTime.plusSeconds(1),
                TrackingTelemetryIngestedV2.TamperState.CLEAR, null, null,
                TrackingTelemetryIngestedV2.ExternalPowerState.UNKNOWN,
                TrackingTelemetryIngestedV2.BatteryChargingState.UNKNOWN);
        return new LiveTelemetryProjection(event, sourceTime.plusSeconds(1));
    }
}
