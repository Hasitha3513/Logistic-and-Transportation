package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.DependencyUnavailableException;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

@Testcontainers
class JdbcHistoricalTelemetryStoreTimescaleAcceptanceTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:latest-pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("tracking_history_acceptance")
            .withUsername("transport_test")
            .withPassword("transport_test");

    private static JdbcTemplate jdbc;
    private static JdbcHistoricalTelemetryStore store;
    private static JdbcTelemetryEvaluationDispatchRepository dispatches;

    @BeforeAll
    static void migrate() {
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
        jdbc.update("DELETE FROM tracking_telemetry_evaluation_dispatch");
        jdbc.update("DELETE FROM tracking_position_history");
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

    private static Map<String, String> placeholders() {
        return com.transportlogistics.app.tracking.HybridTelemetryTimescaleMigrationAcceptanceTest
                .placeholdersForTs04();
    }
}
