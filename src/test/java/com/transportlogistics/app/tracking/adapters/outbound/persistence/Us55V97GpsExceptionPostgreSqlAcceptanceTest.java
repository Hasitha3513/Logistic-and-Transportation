package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.support.AcceptanceDatabaseGuard;
import com.transportlogistics.app.tracking.HybridTelemetryTimescaleMigrationAcceptanceTest;
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

class Us55V97GpsExceptionPostgreSqlAcceptanceTest {
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
    void cleanV1ToV97CreatesExactlyTheAuthorizedReadyTenantIndexes() {
        cleanAndMigrate();

        assertThat(version()).isEqualTo("104");
        assertThat(jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname='public' "
                + "AND tablename LIKE 'tracking_gps_exception_%' ORDER BY tablename", String.class))
                .containsExactly("tracking_gps_exception_acknowledgement_command",
                        "tracking_gps_exception_episode", "tracking_gps_exception_evidence");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_index i JOIN pg_class c "
                + "ON c.oid=i.indexrelid WHERE c.relname IN "
                + "('uq_tracking_gps_exception_active','idx_tracking_gps_exception_tenant_list',"
                + "'idx_tracking_gps_exception_evidence_tenant_episode') "
                + "AND i.indisready AND i.indisvalid", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE schemaname='public' "
                + "AND indexname IN ('uq_tracking_gps_exception_active',"
                + "'idx_tracking_gps_exception_tenant_list',"
                + "'idx_tracking_gps_exception_evidence_tenant_episode')", String.class))
                .allMatch(definition -> definition.contains("tenant_id"));
    }

    @Test
    void v96ToV97IsAtomicAndNormalRetrySucceeds() {
        flyway.clean();
        Flyway.configure().dataSource(jdbc.getDataSource()).target("96")
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                .load().migrate();
        jdbc.execute("CREATE TABLE tracking_gps_exception_evidence(probe INTEGER)");

        assertThatThrownBy(flyway::migrate).isInstanceOf(RuntimeException.class);
        assertThat(version()).isEqualTo("96");
        assertThat(jdbc.queryForObject("SELECT to_regclass('tracking_gps_exception_episode') IS NULL",
                Boolean.class)).isTrue();
        jdbc.execute("DROP TABLE tracking_gps_exception_evidence");

        flyway.migrate();
        assertThat(version()).isEqualTo("104");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history "
                + "WHERE version='97' AND success", Integer.class)).isOne();
    }

    @Test
    void activeEpisodeIsTenantScopedUniqueAndEvidenceIsAppendOnly() {
        cleanAndMigrate();
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID deviceA = device(tenantA, "a");
        UUID deviceB = device(tenantB, "a");
        UUID episodeA = episode(tenantA, deviceA);
        episode(tenantB, deviceB);

        assertThatThrownBy(() -> episode(tenantA, deviceA)).isInstanceOf(RuntimeException.class);
        UUID evidence = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_gps_exception_evidence(
                 id,tenant_id,episode_id,evidence_identity,assessed_at,trust,
                 ordering_classification,reliability_state,quality_codes,transition)
                VALUES(?,?,?, ?,?,'UNTRUSTED','IN_ORDER','DEGRADED','LOW_ACCURACY','OPENED')
                """, evidence, tenantA, episodeA, "a".repeat(64), Timestamp.from(Instant.now()));
        assertThatThrownBy(() -> jdbc.update("DELETE FROM tracking_gps_exception_evidence WHERE id=?",
                evidence)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_gps_exception_episode "
                + "WHERE tenant_id=?", Integer.class, tenantA)).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_gps_exception_episode "
                + "WHERE tenant_id=? AND tracking_device_id=?", Integer.class, tenantB, deviceA)).isZero();
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

    private static UUID device(UUID tenant, String suffix) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,
                 lifecycle,registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'GENERIC','ACTIVE',?,?,0,?,?)
                """, id, tenant, "v97-" + suffix, Timestamp.from(now), UUID.randomUUID(),
                Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private static UUID episode(UUID tenant, UUID device) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO tracking_gps_exception_episode(
                 id,tenant_id,tracking_device_id,exception_type,severity,status,opened_at,
                 last_observed_at,evidence_count,consecutive_recovery_points,version)
                VALUES(?,?,?,'LOW_ACCURACY','WARNING','OPEN',?,?,1,0,0)
                """, id, tenant, device, Timestamp.from(now), Timestamp.from(now));
        return id;
    }
}
