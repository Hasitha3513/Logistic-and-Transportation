package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV3;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.*;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayCursorPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.transportlogistics.app.support.AcceptanceDatabaseGuard;

@Testcontainers
class JdbcHistoricalTelemetryStoreTimescaleAcceptanceTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:latest-pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName(AcceptanceDatabaseGuard.REQUIRED_DATABASE)
            .withUsername("transport_test")
            .withPassword("transport_test");

    private static JdbcTemplate jdbc;
    private static JdbcHistoricalTelemetryStore store;
    private static JdbcTelemetryEvaluationDispatchRepository dispatches;

    @BeforeAll
    static void migrate() {
        var safetyDataSource = new DriverManagerDataSource(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        AcceptanceDatabaseGuard.verify(safetyDataSource);
        Flyway.configure().dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
                .placeholders(placeholders()).load().migrate();
        var dataSource = new DriverManagerDataSource(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        dispatches = new JdbcTelemetryEvaluationDispatchRepository(jdbc, transactions);
        store = new JdbcHistoricalTelemetryStore(jdbc, transactions, dispatches);
    }

    @BeforeEach
    void clear() {
        jdbc.execute("TRUNCATE tracking_telemetry_evaluation_dispatch,tracking_position_history");
    }

    @Test
    void persistsIdempotentlyAndKeepsTenantHistoryIsolated() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant now = Instant.now().minusSeconds(10);
        var a = event(tenantA, vehicle, "a".repeat(64), now, BigDecimal.ONE,
                BigDecimal.TWO, BigDecimal.ZERO, EngineState.OFF, BigDecimal.ONE);
        var b = event(tenantB, vehicle, "a".repeat(64), now, BigDecimal.ONE,
                BigDecimal.TWO, BigDecimal.ZERO, EngineState.OFF, BigDecimal.ONE);

        assertThat(store.persist(List.of(a)).persisted()).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_telemetry_evaluation_dispatch "
                + "WHERE tenant_id=?", Integer.class, tenantA)).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT evaluator_type FROM tracking_telemetry_evaluation_dispatch "
                + "WHERE tenant_id=?", String.class, tenantA))
                .containsExactlyInAnyOrder("GEOFENCE", "SPEED", "ROUTE_DEVIATION");
        assertThat(store.persist(List.of(a)).duplicate()).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_telemetry_evaluation_dispatch "
                + "WHERE tenant_id=?", Integer.class, tenantA)).isEqualTo(3);
        assertThat(store.persist(List.of(b)).persisted()).isOne();
        assertThat(store.find(tenantA, vehicle, now.minusSeconds(1), now.plusSeconds(1), 10))
                .hasSize(1).allMatch(fact -> fact.tenantId().equals(tenantA));
        assertThat(store.find(UUID.randomUUID(), vehicle, now.minusSeconds(1), now.plusSeconds(1), 10))
                .isEmpty();
    }

    @Test
    void v1AndV2ShareTheSameTenantScopedIngestionIdentity() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(10);
        String dedupe = "f".repeat(64);
        var v1 = event(tenant, vehicle, dedupe, source, BigDecimal.ONE,
                BigDecimal.TWO, BigDecimal.ZERO, EngineState.OFF, BigDecimal.ONE);
        var v2 = new TrackingTelemetryIngestedV2(v1.eventId(), TrackingTelemetryIngestedV2.TYPE,
                TrackingTelemetryIngestedV2.VERSION, tenant, vehicle, v1.deviceId(),
                v1.providerAlias(), v1.providerMessageId(), dedupe, v1.latitude(), v1.longitude(),
                v1.speedKph(), v1.headingDegrees(), v1.horizontalAccuracyMeters(),
                v1.altitudeMeters(), v1.engineState(), v1.odometerKm(), v1.engineHours(),
                v1.recordedAt(), v1.receivedAt(), TrackingTelemetryIngestedV2.TamperState.CLEAR,
                BigDecimal.ZERO, new BigDecimal("3.920000"),
                TrackingTelemetryIngestedV2.ExternalPowerState.CONNECTED,
                TrackingTelemetryIngestedV2.BatteryChargingState.CHARGING);

        assertThat(store.persist(List.of(v1)).persisted()).isOne();
        assertThat(store.persist(List.of(v2)).duplicate()).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_position_history "
                + "WHERE tenant_id=? AND dedupe_identity=?", Integer.class, tenant, dedupe))
                .isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_telemetry_evaluation_dispatch "
                + "WHERE tenant_id=?", Integer.class, tenant)).isEqualTo(3);
    }

    @Test
    void persistsV2OptionalEvidenceAndDistinguishesAbsentFromExplicitUnknown() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(10);
        var base = event(tenant, vehicle, "9".repeat(64), source, BigDecimal.ONE,
                BigDecimal.TWO, BigDecimal.ZERO, EngineState.OFF, BigDecimal.ONE);
        var explicit = new TrackingTelemetryIngestedV2(base.eventId(), TrackingTelemetryIngestedV2.TYPE,
                TrackingTelemetryIngestedV2.VERSION, tenant, vehicle, base.deviceId(), "FLESPI",
                "v2-explicit", base.dedupeIdentity(), base.latitude(), base.longitude(),
                base.speedKph(), base.headingDegrees(), base.horizontalAccuracyMeters(),
                base.altitudeMeters(), base.engineState(), base.odometerKm(), base.engineHours(),
                base.recordedAt(), base.receivedAt(), TrackingTelemetryIngestedV2.TamperState.UNKNOWN,
                new BigDecimal("0.000"), new BigDecimal("1000.000000"),
                TrackingTelemetryIngestedV2.ExternalPowerState.UNKNOWN,
                TrackingTelemetryIngestedV2.BatteryChargingState.UNKNOWN);
        var absent = new TrackingTelemetryIngestedV2(UUID.randomUUID(), TrackingTelemetryIngestedV2.TYPE,
                TrackingTelemetryIngestedV2.VERSION, tenant, vehicle, UUID.randomUUID(), "TRACCAR",
                "v2-absent", "8".repeat(64), base.latitude(), base.longitude(), base.speedKph(),
                null, base.horizontalAccuracyMeters(), null, EngineState.UNKNOWN, null, null,
                source.plusSeconds(1), source.plusSeconds(2), null, null, null, null, null);

        assertThat(store.persist(List.of(explicit, absent)).persisted()).isEqualTo(2);
        assertThat(jdbc.queryForMap("SELECT event_version,tamper_state,battery_level_percent,"
                + "battery_voltage_volts,external_power_state,battery_charging_state "
                + "FROM tracking_position_history WHERE tenant_id=? AND id=?", tenant,
                explicit.eventId()))
                .containsEntry("event_version", 2)
                .containsEntry("tamper_state", "UNKNOWN")
                .containsEntry("battery_level_percent", new BigDecimal("0.000"))
                .containsEntry("battery_voltage_volts", new BigDecimal("1000.000000"))
                .containsEntry("external_power_state", "UNKNOWN")
                .containsEntry("battery_charging_state", "UNKNOWN");
        HistoricalTelemetry absentFact = store.findExact(tenant, absent.recordedAt(), absent.eventId())
                .orElseThrow();
        assertThat(absentFact.tamperState()).isNull();
        assertThat(absentFact.batteryLevelPercent()).isNull();
        assertThat(absentFact.batteryVoltageVolts()).isNull();
        assertThat(absentFact.externalPowerState()).isNull();
        assertThat(absentFact.batteryChargingState()).isNull();
    }

    @Test
    void persistsAndRetrievesV3EngineSemanticsLosslesslyWithoutReducingDistinctEvidence() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(10);
        var running = v3(tenant, vehicle, "7".repeat(64), source,
                TrackingTelemetryIngestedV3.EngineRunningState.RUNNING,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_RPM);
        var stopped = v3(tenant, vehicle, "6".repeat(64), source.plusSeconds(1),
                TrackingTelemetryIngestedV3.EngineRunningState.NOT_RUNNING,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_RPM);

        assertThat(store.persist(List.of(running, stopped)).persisted()).isEqualTo(2);
        HistoricalTelemetry fact = store.findExact(tenant, source, running.eventId()).orElseThrow();
        assertThat(fact.eventVersion()).isEqualTo(3);
        assertThat(fact.ignitionState()).isEqualTo(TrackingTelemetryIngestedV3.IgnitionState.ON);
        assertThat(fact.engineRunningState())
                .isEqualTo(TrackingTelemetryIngestedV3.EngineRunningState.RUNNING);
        assertThat(fact.engineRunningSource())
                .isEqualTo(TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_RPM);
        assertThat(fact.tamperState()).isEqualTo(TrackingTelemetryIngestedV2.TamperState.UNKNOWN);
    }

    @Test
    void enqueuesIdleOnlyForSupportedV3EngineRunningEvidence() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID unsupportedVehicle = UUID.randomUUID();
        UUID supportedDevice = device(tenant, "idle-supported");
        UUID unsupportedDevice = device(tenant, "idle-unsupported");
        Instant source = Instant.now().minusSeconds(10);
        jdbc.update("""
                INSERT INTO tracking_device_telemetry_capability(
                 id,tenant_id,tracking_device_id,capability,capability_state,effective_from,
                 recorded_at,recorded_by)
                VALUES(?,?,?,'ENGINE_RUNNING','SUPPORTED',?,?,?)
                """, UUID.randomUUID(), tenant, supportedDevice, java.sql.Timestamp.from(source),
                java.sql.Timestamp.from(source), UUID.randomUUID());
        var supported = v3(tenant, vehicle, supportedDevice, "5".repeat(64), source,
                TrackingTelemetryIngestedV3.EngineRunningState.RUNNING,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_CAN);
        var unsupported = v3(tenant, unsupportedVehicle, unsupportedDevice, "4".repeat(64),
                source.plusSeconds(1), TrackingTelemetryIngestedV3.EngineRunningState.RUNNING,
                TrackingTelemetryIngestedV3.EngineRunningSource.DEVICE_NATIVE_CAN);

        assertThat(store.persist(List.of(supported, unsupported)).persisted()).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT evaluator_type FROM "
                + "tracking_telemetry_evaluation_dispatch WHERE history_id=? ORDER BY evaluator_type",
                String.class, supported.eventId())).containsExactly(
                        "GEOFENCE", "IDLE", "ROUTE_DEVIATION", "SPEED");
        assertThat(jdbc.queryForList("SELECT evaluator_type FROM "
                + "tracking_telemetry_evaluation_dispatch WHERE history_id=? ORDER BY evaluator_type",
                String.class, unsupported.eventId())).containsExactly(
                        "GEOFENCE", "ROUTE_DEVIATION", "SPEED");
        Instant claimTime = Instant.now();
        assertThat(dispatches.claim("existing-worker", claimTime, claimTime.plusSeconds(30), 100))
                .noneMatch(item -> item.evaluator()
                        == com.transportlogistics.app.tracking.ports.outbound
                                .TelemetryEvaluationDispatchPort.Evaluator.IDLE);
        assertThat(dispatches.claimIdle("future-idle-worker", claimTime, claimTime.plusSeconds(30), 100))
                .singleElement().satisfies(item -> assertThat(item.historyId())
                        .isEqualTo(supported.eventId()));
    }

    @Test
    void preservesOutOfOrderAndDistinctSameTimestampEvents() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant newer = Instant.now().minusSeconds(5);
        var first = event(tenant, vehicle, "1".repeat(64), newer, BigDecimal.ONE,
                BigDecimal.TWO, BigDecimal.ONE, EngineState.ON, BigDecimal.ONE);
        var older = event(tenant, vehicle, "2".repeat(64), newer.minusSeconds(60), BigDecimal.TEN,
                BigDecimal.TWO, BigDecimal.ONE, EngineState.ON, BigDecimal.ONE);
        var sameTimeDistinct = event(tenant, vehicle, "3".repeat(64), newer, BigDecimal.TEN,
                BigDecimal.TWO, BigDecimal.ONE, EngineState.ON, BigDecimal.ONE);

        assertThat(store.persist(List.of(first, older, sameTimeDistinct)).persisted()).isEqualTo(3);
        assertThat(store.find(tenant, vehicle, newer.minusSeconds(61), newer.plusSeconds(1), 10))
                .hasSize(3);
    }

    @Test
    void reducesOnlyExactStaticConsecutiveFactsAndRetainsMeaningfulChanges() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant base = Instant.now().minusSeconds(20);
        var first = event(tenant, vehicle, "1".repeat(64), base, BigDecimal.ONE,
                BigDecimal.TWO, BigDecimal.ZERO, EngineState.OFF, BigDecimal.ONE);
        var staticReplay = event(tenant, vehicle, "2".repeat(64), base.plusSeconds(1),
                new BigDecimal("1.0"), new BigDecimal("2.00"), BigDecimal.ZERO,
                EngineState.OFF, BigDecimal.ONE);
        var meterChanged = event(tenant, vehicle, "3".repeat(64), base.plusSeconds(2),
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ZERO, EngineState.OFF, BigDecimal.TEN);
        var engineUnknown = event(tenant, vehicle, "4".repeat(64), base.plusSeconds(3),
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ZERO, null, BigDecimal.TEN);

        var result = store.persist(List.of(first, staticReplay, meterChanged, engineUnknown));

        assertThat(result.persisted()).isEqualTo(3);
        assertThat(result.reduced()).isOne();
    }

    @Test
    void databaseFailureRollsBackWholeBatch() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant base = Instant.now().minusSeconds(10);
        var valid = event(tenant, vehicle, "a".repeat(64), base, BigDecimal.ONE,
                BigDecimal.TWO, BigDecimal.ONE, EngineState.ON, BigDecimal.ONE);
        var invalid = new TrackingTelemetryIngestedV1(UUID.randomUUID(),
                TrackingTelemetryIngestedV1.TYPE, 1, tenant, vehicle, UUID.randomUUID(),
                "X".repeat(81), null, "b".repeat(64), BigDecimal.ONE, BigDecimal.TWO,
                BigDecimal.ONE, null, BigDecimal.ONE, null, EngineState.ON,
                BigDecimal.ONE, null, base.plusSeconds(1), base.plusSeconds(2));

        assertThatThrownBy(() -> store.persist(List.of(valid, invalid)))
                .isInstanceOf(DependencyUnavailableException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_position_history", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_telemetry_evaluation_dispatch",
                Integer.class)).isZero();
    }

    @Test
    void claimsOnceAndSupportsCompletionRetryFailureAndExpiredLeaseRecovery() {
        UUID tenant = UUID.randomUUID();
        Instant now = Instant.now().minusSeconds(10);
        var first = event(tenant, UUID.randomUUID(), "c".repeat(64), now, BigDecimal.ONE,
                BigDecimal.TWO, BigDecimal.ONE, EngineState.ON, BigDecimal.ONE);
        store.persist(List.of(first));

        var claimed = dispatches.claim("worker-a", now.plusSeconds(20), now.plusSeconds(50), 100);

        assertThat(claimed).hasSize(3).extracting(item -> item.evaluator().name())
                .containsExactly("GEOFENCE", "ROUTE_DEVIATION", "SPEED");
        assertThat(dispatches.claim("worker-b", now.plusSeconds(21), now.plusSeconds(51), 100))
                .isEmpty();
        dispatches.complete(claimed.get(0).id(), "worker-a", now.plusSeconds(22));
        dispatches.retry(claimed.get(1).id(), "worker-a", now.plusSeconds(22),
                now.plusSeconds(82), "EVALUATOR_FAILED");
        dispatches.fail(claimed.get(2).id(), "worker-a", now.plusSeconds(22),
                "EVALUATOR_FAILED");
        assertThat(jdbc.queryForList("SELECT status FROM tracking_telemetry_evaluation_dispatch "
                + "WHERE tenant_id=? ORDER BY status", String.class, tenant))
                .containsExactly("COMPLETED", "FAILED", "PENDING");

        var second = event(tenant, UUID.randomUUID(), "d".repeat(64), now.plusSeconds(1),
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, EngineState.ON, BigDecimal.ONE);
        store.persist(List.of(second));
        var abandoned = dispatches.claim("worker-a", now.plusSeconds(23), now.plusSeconds(24), 3);
        assertThat(abandoned).hasSize(3);
        var recovered = dispatches.claim("worker-b", now.plusSeconds(25), now.plusSeconds(55), 3);
        assertThat(recovered).extracting(item -> item.id())
                .containsExactlyElementsOf(abandoned.stream().map(item -> item.id()).toList());
    }

    @Test
    void journeyReplayReadsACompressedChunkWithoutChangingPolicies() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(10L * 86_400L);
        assertThat(store.persist(List.of(event(tenant, vehicle, "e".repeat(64), source,
                BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ONE, EngineState.ON,
                BigDecimal.ONE))).persisted()).isOne();
        jdbc.query("SELECT compress_chunk(chunk) FROM show_chunks('tracking_position_history',"
                + " older_than => now() - interval '7 days') chunk", row -> { });
        var adapter = new JdbcJourneyReplayHistoryAdapter(jdbc,
                new DataSourceTransactionManager(jdbc.getDataSource()),
                mock(JourneyReplayCursorPort.class), Clock.fixed(Instant.now(), ZoneOffset.UTC));
        TimeRange range = new TimeRange(source.minusSeconds(1), source.plusSeconds(2));
        ReplayQuery query = new ReplayQuery(new TenantContext(tenant, UUID.randomUUID()),
                ReplaySelector.vehicle(vehicle), range, range, 10, null, Set.of(),
                Direction.CHRONOLOGICAL_ASCENDING, 20_000);

        assertThat(adapter.query(query, vehicle, null).items()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM timescaledb_information.jobs WHERE "
                + "hypertable_name='tracking_position_history' AND proc_name IN "
                + "('policy_compression','policy_retention')", Integer.class)).isEqualTo(2);
    }

    private static TrackingTelemetryIngestedV1 event(
            UUID tenant, UUID vehicle, String dedupe, Instant recordedAt,
            BigDecimal latitude, BigDecimal longitude, BigDecimal speed,
            EngineState engineState, BigDecimal odometer) {
        return new TrackingTelemetryIngestedV1(UUID.nameUUIDFromBytes(
                (tenant + dedupe).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                TrackingTelemetryIngestedV1.TYPE, 1, tenant, vehicle, UUID.randomUUID(),
                "GENERIC", null, dedupe, latitude, longitude, speed, null,
                BigDecimal.ONE, null, engineState, odometer, null,
                recordedAt, recordedAt.plusSeconds(1));
    }

    private static TrackingTelemetryIngestedV3 v3(
            UUID tenant, UUID vehicle, String dedupe, Instant recordedAt,
            TrackingTelemetryIngestedV3.EngineRunningState running,
            TrackingTelemetryIngestedV3.EngineRunningSource source) {
        return v3(tenant, vehicle, UUID.randomUUID(), dedupe, recordedAt, running, source);
    }

    private static TrackingTelemetryIngestedV3 v3(
            UUID tenant, UUID vehicle, UUID device, String dedupe, Instant recordedAt,
            TrackingTelemetryIngestedV3.EngineRunningState running,
            TrackingTelemetryIngestedV3.EngineRunningSource source) {
        return new TrackingTelemetryIngestedV3(UUID.nameUUIDFromBytes(
                (tenant + dedupe).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                TrackingTelemetryIngestedV3.TYPE, 3, tenant, vehicle, device,
                "TEST_FIXTURE", null, dedupe, BigDecimal.ONE, BigDecimal.TWO, BigDecimal.ZERO,
                null, BigDecimal.ONE, null, EngineState.ON, null, null, recordedAt,
                recordedAt.plusSeconds(1), TrackingTelemetryIngestedV2.TamperState.UNKNOWN,
                null, null, null, null, TrackingTelemetryIngestedV3.IgnitionState.ON,
                running, source);
    }

    private static UUID device(UUID tenant, String reference) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,
                 lifecycle,registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'TEST_FIXTURE','ACTIVE',?,?,0,?,?)
                """, id, tenant, reference, java.sql.Timestamp.from(now), UUID.randomUUID(),
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        return id;
    }

    private static Map<String, String> placeholders() {
        return com.transportlogistics.app.tracking.HybridTelemetryTimescaleMigrationAcceptanceTest
                .placeholdersForTs04();
    }
}
