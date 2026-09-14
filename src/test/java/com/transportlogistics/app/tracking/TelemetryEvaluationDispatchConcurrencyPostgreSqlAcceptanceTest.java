package com.transportlogistics.app.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryEvaluationDispatchPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "app.tracking.hybrid-storage.enabled=true",
        "app.tracking.hybrid-storage.evaluation-delay=3600000"
})
class TelemetryEvaluationDispatchConcurrencyPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired TelemetryEvaluationDispatchPort dispatches;
    @Autowired JdbcTemplate jdbc;

    @Test
    void concurrentWorkersClaimDistinctBoundedBatchesWithoutDeadlock() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        seed(tenantA, 6);
        seed(tenantB, 6);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> claimAfter(barrier, "cs07-worker-a", 6));
            var second = executor.submit(() -> claimAfter(barrier, "cs07-worker-b", 6));
            List<TelemetryEvaluationDispatchPort.Dispatch> a = first.get(10, TimeUnit.SECONDS);
            List<TelemetryEvaluationDispatchPort.Dispatch> b = second.get(10, TimeUnit.SECONDS);
            assertThat(a).hasSize(6);
            assertThat(b).hasSize(6);
            assertThat(a).extracting(TelemetryEvaluationDispatchPort.Dispatch::id)
                    .doesNotContainAnyElementsOf(b.stream()
                            .map(TelemetryEvaluationDispatchPort.Dispatch::id).toList());
            var claimed = new HashSet<UUID>();
            a.forEach(item -> claimed.add(item.id()));
            b.forEach(item -> claimed.add(item.id()));
            assertThat(claimed).hasSize(12);
            assertThat(a).allMatch(item -> item.tenantId().equals(tenantA) || item.tenantId().equals(tenantB));
            assertThat(b).allMatch(item -> item.tenantId().equals(tenantA) || item.tenantId().equals(tenantB));
        }
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_stat_database WHERE datname=current_database() AND deadlocks=0
                """, Integer.class)).isOne();
    }

    @Test
    void activeLeaseCannotBeStolenExpiredLeaseRecoversAndCompletedWorkStaysTerminal() {
        UUID tenant = UUID.randomUUID();
        seed(tenant, 1);
        var first = dispatches.claim("cs07-owner", NOW, NOW.plusSeconds(30), 1).getFirst();
        assertThat(dispatches.claim("cs07-thief", NOW.plusSeconds(29), NOW.plusSeconds(59), 1)).isEmpty();
        var recovered = dispatches.claim("cs07-recovery", NOW.plusSeconds(31), NOW.plusSeconds(61), 1);
        assertThat(recovered).singleElement().extracting(TelemetryEvaluationDispatchPort.Dispatch::id)
                .isEqualTo(first.id());
        dispatches.complete(first.id(), "cs07-recovery", NOW.plusSeconds(32));
        assertThat(dispatches.claim("cs07-replay", NOW.plusSeconds(90), NOW.plusSeconds(120), 1)).isEmpty();
        assertThatThrownBy(() -> dispatches.complete(first.id(), "cs07-owner", NOW.plusSeconds(33)))
                .isInstanceOf(IllegalStateException.class).hasMessage("Dispatch lease lost");
        assertThat(jdbc.queryForMap("""
                SELECT status,attempt_count,lease_owner,lease_until FROM tracking_telemetry_evaluation_dispatch
                WHERE dispatch_id=?
                """, first.id())).containsEntry("status", "COMPLETED")
                .containsEntry("attempt_count", 2).containsEntry("lease_owner", null)
                .containsEntry("lease_until", null);
    }

    @Test
    void duplicateLogicalDispatchConvergesPerTenantAndEvaluatorButNotAcrossTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID history = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        insert(tenantA, history, vehicle, "ROUTE_DEVIATION", "a".repeat(64), 0);
        jdbc.update("""
                INSERT INTO tracking_telemetry_evaluation_dispatch(
                 tenant_id,source_timestamp,history_id,dedupe_identity,vehicle_id,evaluator_type,
                 status,attempt_count,next_attempt_at)
                VALUES(?,?,?,?,?,?,'PENDING',0,?) ON CONFLICT DO NOTHING
                """, tenantA, Timestamp.from(NOW), history, "a".repeat(64), vehicle,
                "ROUTE_DEVIATION", Timestamp.from(NOW));
        insert(tenantB, history, vehicle, "ROUTE_DEVIATION", "a".repeat(64), 0);

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tracking_telemetry_evaluation_dispatch
                WHERE history_id=? AND evaluator_type='ROUTE_DEVIATION'
                """, Integer.class, history)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(DISTINCT tenant_id) FROM tracking_telemetry_evaluation_dispatch
                WHERE history_id=?
                """, Integer.class, history)).isEqualTo(2);
    }

    private List<TelemetryEvaluationDispatchPort.Dispatch> claimAfter(
            CyclicBarrier barrier, String owner, int limit) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        return dispatches.claim(owner, NOW, NOW.plusSeconds(30), limit);
    }

    private void seed(UUID tenant, int count) {
        for (int index = 0; index < count; index++) {
            UUID identity = UUID.randomUUID();
            insert(tenant, identity, UUID.randomUUID(), "ROUTE_DEVIATION", hex(identity), index);
        }
    }

    private void insert(UUID tenant, UUID history, UUID vehicle,
            String evaluator, String dedupe, int offset) {
        jdbc.update("""
                INSERT INTO tracking_telemetry_evaluation_dispatch(
                 tenant_id,source_timestamp,history_id,dedupe_identity,vehicle_id,evaluator_type,
                 status,attempt_count,next_attempt_at)
                VALUES(?,?,?,?,?,?,'PENDING',0,?)
                """, tenant, Timestamp.from(NOW.plusMillis(offset)), history, dedupe, vehicle,
                evaluator, Timestamp.from(NOW));
    }

    private static String hex(UUID value) {
        return value.toString().replace("-", "") + value.toString().replace("-", "");
    }
}
