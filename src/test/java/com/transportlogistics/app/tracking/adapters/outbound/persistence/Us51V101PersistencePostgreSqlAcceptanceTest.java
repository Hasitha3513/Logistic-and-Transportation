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

class Us51V101PersistencePostgreSqlAcceptanceTest {
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
        flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false)
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                .load();
    }

    @AfterAll
    static void stop() {
        DATABASE.stop();
    }

    @Test
    void cleanV1ToV101CreatesOnlyTheAuthorizedV3Boundary() {
        cleanAndMigrate();

        assertThat(version()).isEqualTo("103");
        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema='public' AND table_name='tracking_position_history'
                  AND column_name IN ('ignition_state','engine_running_state','engine_running_source')
                ORDER BY column_name
                """, String.class)).containsExactly(
                        "engine_running_source", "engine_running_state", "ignition_state");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history "
                + "WHERE version='101' AND success", Integer.class)).isOne();
    }

    @Test
    void v100ToV101PreservesLegacyRowsIncludingCompressedHistory() {
        migrateTo100();
        UUID tenant = UUID.randomUUID();
        UUID history = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(10L * 86_400L);
        insertLegacy(tenant, history, source, 2);
        jdbc.query("SELECT compress_chunk(chunk, if_not_compressed => true) "
                + "FROM show_chunks('tracking_position_history',"
                + " older_than => now() - interval '7 days') chunk", row -> { });

        flyway.migrate();

        assertThat(version()).isEqualTo("103");
        assertThat(jdbc.queryForMap("SELECT event_version,engine_state,ignition_state,"
                + "engine_running_state,engine_running_source FROM tracking_position_history "
                + "WHERE tenant_id=? AND id=?", tenant, history))
                .containsEntry("event_version", 2)
                .containsEntry("engine_state", "ON")
                .containsEntry("ignition_state", null)
                .containsEntry("engine_running_state", null)
                .containsEntry("engine_running_source", null);
    }

    @Test
    void v3ConstraintsRejectInvalidVersionStateSourceAndLegacyReinterpretation() {
        cleanAndMigrate();
        UUID tenant = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(1);
        insertV3(tenant, UUID.randomUUID(), device, vehicle, source, "a".repeat(64),
                "ON", "RUNNING", "DEVICE_NATIVE_CAN");

        assertThatThrownBy(() -> insertV3(tenant, UUID.randomUUID(), device, vehicle,
                source.plusSeconds(1), "b".repeat(64), "ON", "RUNNING", null))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> insertV3(tenant, UUID.randomUUID(), device, vehicle,
                source.plusSeconds(2), "c".repeat(64), "ON", null, "DEVICE_NATIVE_RPM"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> insertV3(tenant, UUID.randomUUID(), device, vehicle,
                source.plusSeconds(3), "d".repeat(64), "OFF", "RUNNING", "DEVICE_NATIVE_CAN"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tracking_position_history(
                 tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                 dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version,ignition_state)
                VALUES(?,?,?,2,?,?,'FIXTURE',?,?,1,2,'ON','TRUSTED','ACCEPTABLE',
                 'IN_ORDER','EXTERNAL','V101','ON')
                """, tenant, Timestamp.from(source.plusSeconds(4)), UUID.randomUUID(), device,
                vehicle, "e".repeat(64), Timestamp.from(source.plusSeconds(5))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void engineRunningCapabilityIsTenantScopedEffectiveDatedAndAbsentIsUnknown() {
        cleanAndMigrate();
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID deviceA = device(tenantA, "engine-a");
        UUID deviceB = device(tenantB, "engine-a");
        Instant start = Instant.parse("2026-09-18T00:00:00Z");
        capability(tenantA, deviceA, "ENGINE_RUNNING", "SUPPORTED", start,
                start.plusSeconds(60));
        capability(tenantA, deviceA, "ENGINE_RUNNING", "UNSUPPORTED", start.plusSeconds(60), null);
        capability(tenantB, deviceB, "ENGINE_RUNNING", "SUPPORTED", start, null);
        var lookup = new JdbcTelemetryCapabilityLookupAdapter(jdbc);

        assertThat(lookup.resolve(tenantA, deviceA, TelemetrySignalCapability.ENGINE_RUNNING, start))
                .isEqualTo(TelemetryCapabilityState.SUPPORTED);
        assertThat(lookup.resolve(tenantA, deviceA, TelemetrySignalCapability.ENGINE_RUNNING,
                start.plusSeconds(60))).isEqualTo(TelemetryCapabilityState.UNSUPPORTED);
        assertThat(lookup.resolve(tenantA, deviceB, TelemetrySignalCapability.ENGINE_RUNNING, start))
                .isEqualTo(TelemetryCapabilityState.UNKNOWN);
        assertThat(lookup.resolve(UUID.randomUUID(), deviceA,
                TelemetrySignalCapability.ENGINE_RUNNING, start))
                .isEqualTo(TelemetryCapabilityState.UNKNOWN);
    }

    @Test
    void appendOnlyAndCrossVersionDedupeRemainEnforced() {
        cleanAndMigrate();
        UUID tenant = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID history = UUID.randomUUID();
        Instant source = Instant.now().minusSeconds(1);
        insertV3(tenant, history, device, vehicle, source, "f".repeat(64),
                "ON", "UNKNOWN", "DEVICE_NATIVE_STATUS");

        assertThatThrownBy(() -> jdbc.update("UPDATE tracking_position_history "
                + "SET engine_running_state='NOT_RUNNING' WHERE tenant_id=? AND id=?", tenant,
                history)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> insertLegacy(tenant, UUID.randomUUID(), source, 2,
                device, vehicle, "f".repeat(64))).isInstanceOf(RuntimeException.class);
    }

    private static void cleanAndMigrate() {
        flyway.clean();
        flyway.migrate();
        AcceptanceDatabaseGuard.verify(jdbc.getDataSource());
    }

    private static void migrateTo100() {
        flyway.clean();
        Flyway.configure().dataSource(jdbc.getDataSource()).target("100")
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                .load().migrate();
    }

    private static String version() {
        return jdbc.queryForObject("SELECT version FROM flyway_schema_history "
                + "WHERE success ORDER BY installed_rank DESC LIMIT 1", String.class);
    }

    private static void insertV3(UUID tenant, UUID id, UUID device, UUID vehicle, Instant source,
            String dedupe, String ignition, String running, String runningSource) {
        jdbc.update("""
                INSERT INTO tracking_position_history(
                 tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                 dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version,ignition_state,
                 engine_running_state,engine_running_source)
                VALUES(?,?,?,3,?,?,'FIXTURE',?,?,1,2,'ON','TRUSTED','ACCEPTABLE',
                 'IN_ORDER','EXTERNAL','V101',?,?,?)
                """, tenant, Timestamp.from(source), id, device, vehicle, dedupe,
                Timestamp.from(source.plusSeconds(1)), ignition, running, runningSource);
    }

    private static void insertLegacy(UUID tenant, UUID id, Instant source, int version) {
        insertLegacy(tenant, id, source, version, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID().toString().replace("-", "").repeat(2));
    }

    private static void insertLegacy(UUID tenant, UUID id, Instant source, int version, UUID device,
            UUID vehicle, String dedupe) {
        jdbc.update("""
                INSERT INTO tracking_position_history(
                 tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                 dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version)
                VALUES(?,?,?,?,?,?,'FIXTURE',?,?,1,2,'ON','TRUSTED','ACCEPTABLE',
                 'IN_ORDER','EXTERNAL','V100')
                """, tenant, Timestamp.from(source), id, version, device, vehicle, dedupe,
                Timestamp.from(source.plusSeconds(1)));
    }

    private static UUID device(UUID tenant, String reference) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,
                 lifecycle,registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'FIXTURE','ACTIVE',?,?,0,?,?)
                """, id, tenant, reference, Timestamp.from(now), UUID.randomUUID(),
                Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private static void capability(UUID tenant, UUID device, String capability, String state,
            Instant from, Instant to) {
        jdbc.update("""
                INSERT INTO tracking_device_telemetry_capability(
                 id,tenant_id,tracking_device_id,capability,capability_state,effective_from,
                 effective_to,recorded_at,recorded_by)
                VALUES(?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), tenant, device, capability, state, Timestamp.from(from),
                to == null ? null : Timestamp.from(to), Timestamp.from(Instant.now()),
                UUID.randomUUID());
    }
}
