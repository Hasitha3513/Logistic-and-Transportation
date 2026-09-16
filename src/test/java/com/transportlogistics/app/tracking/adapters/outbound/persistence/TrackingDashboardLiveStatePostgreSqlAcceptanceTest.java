package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardFilter;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.DashboardQuery;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Disclosure;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.SourceStatus;
import com.transportlogistics.app.tracking.application.TrackingDashboardQueryService;
import com.transportlogistics.app.tracking.ports.outbound.LiveTelemetryProjectionPort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardLiveStatePort;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardIncidentPort;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
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
    @Autowired TrackingDashboardQueryService dashboard;
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
    void recoversFromRedisFailureWithoutChangingDatabaseFallbackEvidence() {
        UUID vehicle = UUID.randomUUID();
        insert(vehicle, NOW.minusSeconds(30), NOW.minusSeconds(29), "TRUSTED", "6.927", "79.861", "8");
        when(redis.findLive(TENANT, NOW, 500))
                .thenThrow(new DependencyUnavailableException(
                        "TRACKING_LIVE_PROJECTION_UNAVAILABLE", "unavailable", null))
                .thenReturn(List.of());

        var degraded = liveState.find(TENANT, emptyFilter(), null, 50, NOW);
        var recovered = liveState.find(TENANT, emptyFilter(), null, 50, NOW);

        assertThat(degraded.sourceStatus()).isEqualTo(SourceStatus.DEGRADED);
        assertThat(recovered.sourceStatus()).isEqualTo(SourceStatus.AVAILABLE);
        assertThat(recovered.items()).isEqualTo(degraded.items());
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

    @Test
    void twentyConcurrentWarmSessionsRemainBoundedWithinControlledTarget() throws Exception {
        List<UUID> vehicles = java.util.stream.IntStream.range(0, 100)
                .mapToObj(ignored -> UUID.randomUUID()).toList();
        for (int index = 0; index < vehicles.size(); index++) {
            insert(vehicles.get(index), NOW.minusSeconds(index % 50), NOW.minusSeconds(index % 50),
                    "TRUSTED", "6.927", "79.861", "8");
        }
        insertSpeedIncidents(vehicles);
        when(redis.findLive(TENANT, NOW, 500)).thenReturn(List.of());
        var request = new DashboardQuery(TENANT, new DashboardFilter(Set.of(), Set.of(), Set.of(),
                Set.of(), Set.of(), true, true), null, 100);
        var disclosure = new Disclosure(true, true, true, true, true);

        long initialStarted = System.nanoTime();
        var initial = dashboard.query(request, disclosure);
        long initialMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - initialStarted);
        assertThat(initial.vehicles()).hasSize(100);
        assertThat(initial.heatMapCells()).hasSizeLessThanOrEqualTo(100);
        assertThat(initial.incidents()).hasSizeLessThanOrEqualTo(50);
        assertThat(initialMillis).isLessThan(1_500);

        var barrier = new CyclicBarrier(20);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = java.util.stream.IntStream.range(0, 20).mapToObj(ignored -> executor.submit(() -> {
                barrier.await();
                long started = System.nanoTime();
                var result = dashboard.query(request, disclosure);
                assertThat(result.vehicles()).hasSize(100);
                return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            })).toList();
            List<Long> timings = new java.util.ArrayList<>();
            for (var future : futures) timings.add(future.get());
            timings.sort(Long::compareTo);
            long p95 = timings.get((int) Math.ceil(timings.size() * 0.95) - 1);
            System.out.println("US54_CS05_LOAD initialMs=" + initialMillis + " p95Ms=" + p95
                    + " sessions=20 vehicles=100");
            assertThat(p95).isLessThan(1_000);
        }
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
                UUID.randomUUID(), vehicleId, id.toString().replace("-", "").repeat(2),
                OffsetDateTime.ofInstant(received, java.time.ZoneOffset.UTC),
                new BigDecimal(latitude), new BigDecimal(longitude), new BigDecimal(speed), trust,
                OffsetDateTime.ofInstant(source.plusSeconds(86_400), java.time.ZoneOffset.UTC));
    }

    private void insertSpeedIncidents(List<UUID> vehicles) {
        for (int index = 0; index < 200; index++) {
            UUID id = UUID.randomUUID();
            Instant confirmation = NOW.minusSeconds(index * 60L);
            jdbc.update("""
                    INSERT INTO tracking_speed_episode(
                      id,tenant_id,vehicle_id,rule_id,rule_version,threshold_source,
                      effective_threshold_kph,start_source_timestamp,confirmation_source_timestamp,
                      end_source_timestamp,max_observed_speed_kph,eligible_above_threshold_sample_count,
                      severity,repeat_count,first_candidate_position_id,confirming_position_id)
                    VALUES(?,?,?,?,1,'TENANT_CONFIG',80,?,?,?,90,2,?,0,?,?)
                    """, id, TENANT, vehicles.get(index % vehicles.size()), UUID.randomUUID(),
                    Timestamp.from(confirmation.minusSeconds(1)), Timestamp.from(confirmation),
                    Timestamp.from(confirmation.plusSeconds(1)), index % 2 == 0 ? "WARNING" : "HIGH",
                    UUID.randomUUID(), UUID.randomUUID());
        }
        jdbc.execute("ANALYZE tracking_position_history");
        jdbc.execute("ANALYZE tracking_speed_episode");
    }

    private static DashboardFilter emptyFilter() {
        return new DashboardFilter(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), false, false);
    }
}
