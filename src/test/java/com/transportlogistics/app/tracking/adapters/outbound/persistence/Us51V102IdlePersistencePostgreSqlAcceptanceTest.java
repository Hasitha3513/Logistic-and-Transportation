package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.support.AcceptanceDatabaseGuard;
import com.transportlogistics.app.tracking.HybridTelemetryTimescaleMigrationAcceptanceTest;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.CapabilityState;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Episode;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.EpisodeLifecycle;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Evidence;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.EvidenceOutcome;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Mutation;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.PersistResult;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.State;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.StateValue;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class Us51V102IdlePersistencePostgreSqlAcceptanceTest {
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:latest-pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName(AcceptanceDatabaseGuard.REQUIRED_DATABASE)
            .withUsername("transport_test")
            .withPassword("transport_test");
    private static final Instant NOW = Instant.parse("2026-09-18T12:00:00Z");
    private static Flyway flyway;
    private static JdbcTemplate jdbc;
    private static JdbcIdlePersistenceAdapter persistence;

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
        persistence = new JdbcIdlePersistenceAdapter(jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @AfterAll
    static void stop() {
        DATABASE.stop();
    }

    @BeforeEach
    void migrate() {
        flyway.clean();
        flyway.migrate();
        AcceptanceDatabaseGuard.verify(jdbc.getDataSource());
    }

    @Test
    void cleanMigrationCreatesExactAuthorizedBoundaryAndV101UpgradePreservesHistory() {
        assertThat(version()).isEqualTo("102");
        assertThat(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema='public' AND table_name LIKE 'tracking_idle_%'
                ORDER BY table_name
                """, String.class)).containsExactly(
                        "tracking_idle_episode", "tracking_idle_episode_evidence", "tracking_idle_state");
        assertThat(jdbc.queryForList("""
                SELECT indexname FROM pg_indexes WHERE schemaname='public'
                 AND indexname IN ('uq_tracking_idle_episode_open',
                  'idx_tracking_idle_episode_vehicle_keyset','idx_tracking_idle_state_tenant_state')
                ORDER BY indexname
                """, String.class)).hasSize(3);
        assertThat(jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                + "WHERE conname='ck_tracking_telemetry_dispatch_evaluator'", String.class))
                .contains("IDLE").contains("GEOFENCE").contains("SPEED").contains("ROUTE_DEVIATION");

        flyway.clean();
        Flyway.configure().dataSource(jdbc.getDataSource()).target("101")
                .placeholders(HybridTelemetryTimescaleMigrationAcceptanceTest.placeholdersForTs04())
                .load().migrate();
        UUID history = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_position_history(
                 tenant_id,source_timestamp,id,event_version,device_id,vehicle_id,provider_alias,
                 dedupe_identity,received_at,latitude,longitude,engine_state,trust,quality,
                 ordering_classification,retention_policy,retention_policy_version)
                VALUES(?,?,?,2,?,?,'FIXTURE',?,?,1,2,'ON','TRUSTED','ACCEPTABLE',
                 'IN_ORDER','EXTERNAL','V101')
                """, UUID.randomUUID(), Timestamp.from(NOW), history, UUID.randomUUID(),
                UUID.randomUUID(), "a".repeat(64), Timestamp.from(NOW.plusSeconds(1)));
        flyway.migrate();
        assertThat(version()).isEqualTo("102");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_position_history WHERE id=?",
                Integer.class, history)).isOne();
    }

    @Test
    void persistsTenantQualifiedCandidateAtomicallyAndRejectsDuplicateEvidence() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID device = device(tenant, "idle-atomic");
        Mutation mutation = mutation(tenant, vehicle, device, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "b".repeat(64), -1, -1, true);

        assertThat(persistence.persist(mutation)).isEqualTo(PersistResult.APPLIED);
        assertThat(persistence.persist(mutation)).isEqualTo(PersistResult.DUPLICATE);
        assertThat(persistence.findState(tenant, vehicle)).contains(mutation.state());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_idle_episode_evidence "
                + "WHERE tenant_id=? AND episode_id=?", Integer.class, tenant,
                mutation.episode().id())).isOne();
        assertThat(persistence.findState(UUID.randomUUID(), vehicle)).isEmpty();

        assertThatThrownBy(() -> jdbc.update("UPDATE tracking_idle_episode_evidence "
                + "SET outcome='UNKNOWN' WHERE id=?", mutation.evidence().id()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM tracking_idle_episode_evidence WHERE id=?",
                mutation.evidence().id())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void databaseEnforcesOneOpenEpisodeDuringConcurrentCreation() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID device = device(tenant, "idle-race");
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> insertCandidateAfter(barrier, tenant, vehicle, device));
            var second = executor.submit(() -> insertCandidateAfter(barrier, tenant, vehicle, device));
            int successes = first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS);
            assertThat(successes).isOne();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_idle_episode "
                + "WHERE tenant_id=? AND vehicle_id=? AND lifecycle<>'CLOSED'", Integer.class,
                tenant, vehicle)).isOne();
    }

    @Test
    void optimisticConflictAndForeignTenantFailureRollBackEpisodeAndEvidence() {
        UUID tenant = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        UUID device = device(tenant, "idle-rollback");
        Mutation first = mutation(tenant, vehicle, device, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c".repeat(64), -1, -1, true);
        persistence.persist(first);
        Mutation stale = mutation(tenant, vehicle, device, first.episode().id(), UUID.randomUUID(),
                UUID.randomUUID(), "d".repeat(64), 99, 99, false);

        assertThatThrownBy(() -> persistence.persist(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_idle_episode_evidence "
                + "WHERE id=?", Integer.class, stale.evidence().id())).isZero();

        UUID foreignDevice = UUID.randomUUID();
        Mutation foreign = mutation(tenant, UUID.randomUUID(), foreignDevice, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "e".repeat(64), -1, -1, true);
        assertThatThrownBy(() -> persistence.persist(foreign)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tracking_idle_episode WHERE id=?",
                Integer.class, foreign.episode().id())).isZero();
    }

    private int insertCandidateAfter(CyclicBarrier barrier, UUID tenant, UUID vehicle, UUID device)
            throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        try {
            jdbc.update("""
                    INSERT INTO tracking_idle_episode(
                     id,tenant_id,vehicle_id,device_id,lifecycle,start_source_timestamp,
                     last_source_timestamp) VALUES(?,?,?,?,'CANDIDATE',?,?)
                    """, UUID.randomUUID(), tenant, vehicle, device, Timestamp.from(NOW),
                    Timestamp.from(NOW));
            return 1;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static Mutation mutation(UUID tenant, UUID vehicle, UUID device, UUID episodeId,
            UUID evidenceId, UUID historyId, String dedupe, long stateVersion, long episodeVersion,
            boolean create) {
        State state = new State(tenant, vehicle, device, StateValue.CANDIDATE,
                CapabilityState.SUPPORTED, NOW, NOW, NOW, 0, 1, episodeId, dedupe,
                stateVersion < 0 ? 0 : stateVersion + 1);
        Episode episode = new Episode(episodeId, tenant, vehicle, device,
                EpisodeLifecycle.CANDIDATE, NOW, null, NOW, null, null, 0, 1,
                episodeVersion < 0 ? 0 : episodeVersion + 1);
        Evidence evidence = new Evidence(evidenceId, tenant, episodeId, vehicle, device, historyId,
                NOW, dedupe, EvidenceOutcome.QUALIFYING, "RUNNING", "DEVICE_NATIVE_CAN",
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, 0);
        return new Mutation(state, episode, evidence, stateVersion, episodeVersion, create);
    }

    private UUID device(UUID tenant, String reference) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO tracking_device(id,tenant_id,external_device_reference,provider_alias,
                 lifecycle,registered_at,registered_by,version,created_at,updated_at)
                VALUES(?,?,?,'FIXTURE','ACTIVE',?,?,0,?,?)
                """, id, tenant, reference, Timestamp.from(NOW), UUID.randomUUID(),
                Timestamp.from(NOW), Timestamp.from(NOW));
        return id;
    }

    private String version() {
        return jdbc.queryForObject("SELECT version FROM flyway_schema_history "
                + "WHERE success ORDER BY installed_rank DESC LIMIT 1", String.class);
    }
}
