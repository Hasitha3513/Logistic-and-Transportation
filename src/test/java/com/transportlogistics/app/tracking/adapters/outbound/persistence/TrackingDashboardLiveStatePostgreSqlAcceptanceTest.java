package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.SourceStatus;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardLiveStatePort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardIncidentPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("postgres")
class TrackingDashboardLiveStatePostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final UUID TENANT = UUID.fromString("2d7b1f18-935a-45d5-9a8c-cf2290efb243");
    private static final Instant NOW = Instant.parse("2026-09-16T04:00:00Z");

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired TrackingDashboardLiveStatePort liveState;
    @Autowired TrackingDashboardIncidentPort incidents;
    @MockBean LiveTelemetryProjectionPort redis;

    @BeforeEach
    void reset() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void fallsBackTruthfullyAndKeepsLatestTrustedSeparateFromUntrustedReceived() {
        UUID vehicle = UUID.randomUUID();
        insert(vehicle, NOW.minusSeconds(30), NOW.minusSeconds(29), "TRUSTED", "6.927", "79.861", "8");
        insert(vehicle, NOW.minusSeconds(10), NOW.minusSeconds(9), "UNTRUSTED", "7.000", "80.000", "20");
        when(redis.findLive(TENANT, NOW, 500)).thenThrow(new DependencyUnavailableException(
                "TRACKING_LIVE_PROJECTION_UNAVAILABLE", "unavailable", null));

        var result = liveState.find(TENANT, emptyFilter(), null, 50, NOW);

        assertThat(result.sourceStatus()).isEqualTo(SourceStatus.DEGRADED);
        assertThat(result.items()).singleElement().satisfies(state -> {
            assertThat(state.vehicleId()).isEqualTo(vehicle);
            assertThat(state.latestReceived().latitude()).isEqualByComparingTo("7.000");
            assertThat(state.latestReceived().trust().name()).isEqualTo("UNTRUSTED");
            assertThat(state.latestTrusted().latitude()).isEqualByComparingTo("6.927");
            assertThat(state.freshness().name()).isEqualTo("LIVE");
        });
        assertThat(liveState.find(UUID.randomUUID(), emptyFilter(), null, 50, NOW).items()).isEmpty();
    }

    @Test
    void incidentSourcesRemainBoundedTenantScopedAndTruthfullyAvailableWhenEmpty() {
        var result = incidents.find(TENANT, Set.of(), Set.of(), NOW.minusSeconds(86_400), NOW, 20, 50);
        assertThat(result.items()).isEmpty();
        assertThat(result.sourceStatuses()).containsOnly(
                org.assertj.core.api.Assertions.entry(
                        com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels
                                .IncidentType.GEOFENCE, SourceStatus.AVAILABLE),
                org.assertj.core.api.Assertions.entry(
                        com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels
                                .IncidentType.SPEED, SourceStatus.AVAILABLE),
                org.assertj.core.api.Assertions.entry(
                        com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels
                                .IncidentType.ROUTE_DEVIATION, SourceStatus.AVAILABLE));
    }

    private void insert(
            UUID vehicleId, Instant source, Instant received, String trust,
            String latitude, String longitude, String speed) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_position_history(
                  tenant_id, source_timestamp, id, device_id, vehicle_id, provider_alias,
                  dedupe_identity, received_at, latitude, longitude, horizontal_accuracy_meters,
                  speed_kph, engine_state, trust, quality, ordering_classification,
                  retention_policy, retention_policy_version, retain_until)
                VALUES (?, ?, ?, ?, ?, 'ACCEPTANCE', ?, ?, ?, ?, 10, ?, 'UNKNOWN', ?,
                        'ACCEPTABLE', 'IN_ORDER', 'STANDARD', 'V1', ?)
                """, TENANT, OffsetDateTime.ofInstant(source, java.time.ZoneOffset.UTC), id,
                UUID.randomUUID(), vehicleId, "0".repeat(64),
                OffsetDateTime.ofInstant(received, java.time.ZoneOffset.UTC),
                new BigDecimal(latitude), new BigDecimal(longitude), new BigDecimal(speed), trust,
                OffsetDateTime.ofInstant(source.plusSeconds(86_400), java.time.ZoneOffset.UTC));
    }

    private static DashboardFilter emptyFilter() {
        return new DashboardFilter(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), false, false);
    }
}
