package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.support.AcceptanceDatabaseGuard;
import com.transportlogistics.app.tracking.HybridTelemetryTimescaleMigrationAcceptanceTest;
import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class Us55V96PersistencePostgreSqlAcceptanceTest {
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:latest-pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName(AcceptanceDatabaseGuard.REQUIRED_DATABASE)
            .withUsername("transport_test")
            .withPassword("transport_test");
    private static Flyway flyway;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void start() {
        DATABASE.start();
        var dataSource = new DriverManagerDataSource(
                DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        AcceptanceDatabaseGuard.verify(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        flyway = Flyway.configure().dataSource(dataSource)
                .cleanDisabled(false)
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                .load();
    }

    @AfterAll
    static void stop() {
        DATABASE.stop();
    }

    @Test
    void cleanV1ToV96CreatesOnlyTheAuthorizedPersistenceFoundation() {
        cleanAndMigrate();

        assertThat(version()).isEqualTo("98");
        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema='public' AND table_name='tracking_position_history'
                  AND column_name IN ('tamper_state','battery_level_percent','battery_voltage_volts',
                   'external_power_state','battery_charging_state')
                ORDER BY column_name
                """, String.class)).containsExactly("battery_charging_state", "battery_level_percent",
                        "battery_voltage_volts", "external_power_state", "tamper_state");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname='public' "
                + "AND tablename='tracking_device_telemetry_capability' AND indexname IN "
                + "('tracking_device_telemetry_capability_pkey','uq_tracking_device_capability_tenant_id',"
                + "'uq_tracking_device_capability_active','idx_tracking_device_capability_effective')",
                Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_index index "
                + "JOIN pg_class relation ON relation.oid=index.indexrelid "
                + "WHERE relation.relname IN ('uq_tracking_device_capability_active',"
                + "'idx_tracking_device_capability_effective') AND index.indisready AND index.indisvalid",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void v95ToV96PreservesV1HistoryAndMigratesCompressedChunks() {
        flyway.clean();
        Flyway.configure().dataSource(jdbc.getDataSource()).target("95")
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                .load().migrate();
        UUID tenant = UUID.randomUUID();
        UUID history = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(10L * 86_400L);
        jdbc.update("""
                INSERT INTO tracking_position_history(
                 tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                 dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version)
                VALUES(?,?,?,?,?,?,'GENERIC',?,?,1,2,'UNKNOWN','UNKNOWN','ACCURACY_UNKNOWN',
                 'IN_ORDER','EXTERNAL','V95')
                """, tenant, Timestamp.from(source), history, 1, UUID.randomUUID(), UUID.randomUUID(),
                "a".repeat(64), Timestamp.from(source.plusSeconds(1)));
        jdbc.query("SELECT compress_chunk(chunk) FROM show_chunks('tracking_position_history',"
                + " older_than => now() - interval '7 days') chunk", row -> { });

        flyway.migrate();

        assertThat(version()).isEqualTo("98");
        assertThat(jdbc.queryForMap("SELECT event_version,tamper_state,battery_level_percent,"
                + "battery_voltage_volts,external_power_state,battery_charging_state "
                + "FROM tracking_position_history WHERE tenant_id=? AND id=?", tenant, history))
                .containsEntry("event_version", 1)
                .containsEntry("tamper_state", null)
                .containsEntry("battery_level_percent", null);
    }

    @Test
    void migrationFailureRollsBackAllV96ChangesAndNormalRetrySucceeds() {
        flyway.clean();
        Flyway.configure().dataSource(jdbc.getDataSource()).target("95")
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                .load().migrate();
        jdbc.execute("CREATE TABLE tracking_device_telemetry_capability(probe INTEGER)");

        assertThatThrownBy(flyway::migrate).isInstanceOf(RuntimeException.class);
        assertThat(version()).isEqualTo("95");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema='public' AND table_name='tracking_position_history' "
                + "AND column_name='tamper_state'", Integer.class)).isZero();
        jdbc.execute("DROP TABLE tracking_device_telemetry_capability");

        flyway.migrate();
        assertThat(version()).isEqualTo("98");
    }

    @Test
    void capabilityHistoryIsTenantScopedEffectiveDatedAndAppendOnly() {
        cleanAndMigrate();
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID deviceA = device(tenantA, "device-a");
        UUID deviceB = device(tenantB, "device-a");
        UUID actor = UUID.randomUUID();
        Instant start = Instant.parse("2026-09-16T00:00:00Z");
        UUID first = capability(tenantA, deviceA, "TAMPER", "UNKNOWN", start, null, actor);
        capability(tenantB, deviceB, "TAMPER", "SUPPORTED", start, null, actor);
        var lookup = new JdbcTelemetryCapabilityLookupAdapter(jdbc);

        assertThat(lookup.resolve(tenantA, deviceA, TelemetrySignalCapability.TAMPER,
                start)).isEqualTo(TelemetryCapabilityState.UNKNOWN);
        assertThat(lookup.resolve(tenantB, deviceB, TelemetrySignalCapability.TAMPER,
                start)).isEqualTo(TelemetryCapabilityState.SUPPORTED);
        assertThat(lookup.resolve(UUID.randomUUID(), deviceA, TelemetrySignalCapability.TAMPER,
                start)).isEqualTo(TelemetryCapabilityState.UNKNOWN);
        assertThatThrownBy(() -> capability(tenantA, deviceA, "TAMPER", "SUPPORTED",
                start.plusSeconds(1), null, actor)).isInstanceOf(RuntimeException.class);

        Instant boundary = start.plusSeconds(60);
        jdbc.update("UPDATE tracking_device_telemetry_capability SET effective_to=? WHERE id=?",
                Timestamp.from(boundary), first);
        capability(tenantA, deviceA, "TAMPER", "SUPPORTED", boundary, null, actor);
        assertThat(lookup.resolve(tenantA, deviceA, TelemetrySignalCapability.TAMPER,
                boundary.minusMillis(1))).isEqualTo(TelemetryCapabilityState.UNKNOWN);
        assertThat(lookup.resolve(tenantA, deviceA, TelemetrySignalCapability.TAMPER,
                boundary)).isEqualTo(TelemetryCapabilityState.SUPPORTED);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM tracking_device_telemetry_capability "
                + "WHERE id=?", first)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void historyConstraintsPreserveAbsentUnknownAndRejectInvalidEvidence() {
        cleanAndMigrate();
        UUID tenant = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(1);
        insertHistory(tenant, source, UUID.randomUUID(), device, vehicle, "b".repeat(64),
                "UNKNOWN", "0.000", "1000.000000", "UNKNOWN", "UNKNOWN");
        insertHistory(tenant, source.plusSeconds(1), UUID.randomUUID(), device, vehicle,
                "c".repeat(64), null, null, null, null, null);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_position_history "
                + "WHERE tenant_id=? AND event_version=2", Integer.class, tenant)).isEqualTo(2);
        assertThatThrownBy(() -> insertHistory(tenant, source.plusSeconds(2), UUID.randomUUID(),
                device, vehicle, "d".repeat(64), "CLEAR", "100.0001", null, null, null))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> insertHistory(tenant, source.plusSeconds(3), UUID.randomUUID(),
                device, vehicle, "e".repeat(64), "DETECTED", null, "1000.0000001", null, null))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE tracking_position_history SET tamper_state='CLEAR' "
                + "WHERE tenant_id=?", tenant)).isInstanceOf(RuntimeException.class);
    }

    private static void cleanAndMigrate() {
        flyway.clean();
        flyway.migrate();
        AcceptanceDatabaseGuard.verify(jdbc.getDataSource());
    }

    private static String version() {
        return jdbc.queryForObject("SELECT version FROM flyway_schema_history "
                + "WHERE success ORDER BY installed_rank DESC LIMIT 1", String.class);
    }

    private static UUID device(UUID tenant, String reference) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,
                 lifecycle,registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'FLESPI','ACTIVE',?,?,0,?,?)
                """, id, tenant, reference, Timestamp.from(now), UUID.randomUUID(),
                Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private static UUID capability(UUID tenant, UUID device, String capability, String state,
            Instant from, Instant to, UUID actor) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_device_telemetry_capability(
                 id,tenant_id,tracking_device_id,capability,capability_state,effective_from,
                 effective_to,recorded_at,recorded_by)
                VALUES(?,?,?,?,?,?,?,?,?)
                """, id, tenant, device, capability, state, Timestamp.from(from),
                to == null ? null : Timestamp.from(to), Timestamp.from(Instant.now()), actor);
        return id;
    }

    private static void insertHistory(UUID tenant, Instant source, UUID id, UUID device, UUID vehicle,
            String dedupe, String tamper, String level, String voltage, String power, String charging) {
        jdbc.update("""
                INSERT INTO tracking_position_history(
                 tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                 dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version,tamper_state,
                 battery_level_percent,battery_voltage_volts,external_power_state,battery_charging_state)
                VALUES(?,?,?,2,?,?,'FLESPI',?,?,1,2,'UNKNOWN','UNKNOWN','ACCURACY_UNKNOWN',
                 'IN_ORDER','EXTERNAL','V96',?,?::numeric,?::numeric,?,?)
                """, tenant, Timestamp.from(source), id, device, vehicle, dedupe,
                Timestamp.from(source.plusSeconds(1)), tamper, level, voltage, power, charging);
    }
}
