package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class FuelExceptionConcurrencyPostgreSqlAcceptanceTest extends PostgreSqlIntegrationTest {
    @Autowired DataSource dataSource;
    @Autowired JdbcClient jdbc;

    @Test void duplicateManualCaseCreationHasOneActiveWinner() throws Exception {
        UUID tenant = UUID.randomUUID(), source = UUID.randomUUID();
        var results = race(
                connection -> insertCase(connection, tenant, source, null, "SUSPECTED_FUEL_LOSS"),
                connection -> insertCase(connection, tenant, source, null, "SUSPECTED_FUEL_LOSS"));
        assertOneWinner(results);
        assertThat(count("fuel_exception_case", "tenant_id", tenant)).isEqualTo(1);
    }

    @Test void duplicateNegativeAutoCaseHasOneSourceEventAndOneActiveWinner() throws Exception {
        UUID tenant = UUID.randomUUID(), source = UUID.randomUUID(), event = UUID.randomUUID();
        var results = race(
                connection -> insertCase(connection, tenant, source, event, "NEGATIVE_BUNKER_BALANCE"),
                connection -> insertCase(connection, tenant, source, event, "NEGATIVE_BUNKER_BALANCE"));
        assertOneWinner(results);
        assertThat(jdbc.sql("select count(distinct source_event_id) from fuel_exception_case where tenant_id=:t")
                .param("t", tenant).query(Integer.class).single()).isEqualTo(1);
    }

    @Test void twoReviewersUseOptimisticVersion() throws Exception {
        UUID id = seedCase("OPEN");
        assertOneWinner(race(caseTransition(id, "OPEN", "UNDER_REVIEW"), caseTransition(id, "OPEN", "UNDER_REVIEW")));
        assertCase(id, "UNDER_REVIEW", 1);
    }

    @Test void approveVersusRejectProducesOneImmutableDecision() throws Exception {
        UUID correction = seedCorrection(seedCase("AWAITING_APPROVAL"), "AWAITING_APPROVAL", "NOT_STARTED", 0);
        assertOneWinner(race(correctionTransition(correction, "APPROVED"), correctionTransition(correction, "REJECTED")));
        assertThat(status("fuel_exception_correction", correction)).isIn("APPROVED", "REJECTED");
    }

    @Test void doubleApproveProducesOneEffectiveApproval() throws Exception {
        UUID correction = seedCorrection(seedCase("AWAITING_APPROVAL"), "AWAITING_APPROVAL", "NOT_STARTED", 0);
        assertOneWinner(race(correctionTransition(correction, "APPROVED"), correctionTransition(correction, "APPROVED")));
        assertThat(status("fuel_exception_correction", correction)).isEqualTo("APPROVED");
    }

    @Test void resolveVersusCorrectionRequestCannotCreateMixedLifecycle() throws Exception {
        UUID id = seedCase("UNDER_REVIEW");
        assertOneWinner(race(caseTransition(id, "UNDER_REVIEW", "RESOLVED"),
                caseTransition(id, "UNDER_REVIEW", "AWAITING_APPROVAL")));
        assertThat(status("fuel_exception_case", id)).isIn("RESOLVED", "AWAITING_APPROVAL");
    }

    @Test void doubleEscalationCreatesOneDurableHandoff() throws Exception {
        UUID exception = seedCase("OPEN"), tenant = tenant(exception), event = UUID.randomUUID();
        SqlWork insert = connection -> update(connection,
                "insert into fuel_exception_operations_handoff(id,tenant_id,exception_id,handoff_event_id,status,reason,version,created_at,updated_at) values(?,?,?,?, 'PENDING','race',0,?,?)",
                UUID.randomUUID(), tenant, exception, event, OffsetDateTime.now(), OffsetDateTime.now());
        assertOneWinner(race(insert, insert));
        assertThat(count("fuel_exception_operations_handoff", "exception_id", exception)).isEqualTo(1);
    }

    @Test void handoffRetryConcurrencyHasOnePublisherWinner() throws Exception {
        UUID exception = seedCase("OPEN"), tenant = tenant(exception), handoff = UUID.randomUUID();
        jdbc.sql("insert into fuel_exception_operations_handoff(id,tenant_id,exception_id,handoff_event_id,status,reason,version,created_at,updated_at,attempt_count) values(:id,:t,:x,:e,'FAILED','race',0,:now,:now,1)")
                .param("id", handoff).param("t", tenant).param("x", exception).param("e", UUID.randomUUID())
                .param("now", OffsetDateTime.now()).update();
        SqlWork retry = connection -> update(connection,
                "update fuel_exception_operations_handoff set status='PUBLISHED',version=1,attempt_count=2 where id=? and status='FAILED' and version=0", handoff);
        assertOneWinner(race(retry, retry));
        assertThat(jdbc.sql("select attempt_count from fuel_exception_operations_handoff where id=:id")
                .param("id", handoff).query(Integer.class).single()).isEqualTo(2);
    }

    @Test void ownerCommandRetryConcurrencyStartsExecutionOnce() throws Exception {
        UUID correction = seedCorrection(seedCase("CORRECTION_PENDING"), "FAILED", "FAILED", 3);
        SqlWork retry = connection -> update(connection,
                "update fuel_exception_correction set execution_state='RUNNING',version=4 where id=? and status='FAILED' and execution_state='FAILED' and version=3", correction);
        assertOneWinner(race(retry, retry));
        assertThat(jdbc.sql("select execution_state from fuel_exception_correction where id=:id")
                .param("id", correction).query(String.class).single()).isEqualTo("RUNNING");
    }

    private SqlWork caseTransition(UUID id, String expected, String next) {
        return connection -> update(connection,
                "update fuel_exception_case set lifecycle=?,version=1 where id=? and lifecycle=? and version=0", next, id, expected);
    }

    private SqlWork correctionTransition(UUID id, String next) {
        return connection -> update(connection,
                "update fuel_exception_correction set status=?,version=1 where id=? and status='AWAITING_APPROVAL' and version=0", next, id);
    }

    private List<Integer> race(SqlWork first, SqlWork second) throws Exception {
        var ready = new CountDownLatch(2); var go = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> executeReady(first, ready, go));
            var b = executor.submit(() -> executeReady(second, ready, go));
            ready.await(); go.countDown();
            return List.of(a.get(), b.get());
        }
    }

    private int executeReady(SqlWork work, CountDownLatch ready, CountDownLatch go) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ready.countDown(); go.await();
            try { return work.run(connection); } catch (java.sql.SQLException conflict) { return 0; }
        }
    }

    private int insertCase(Connection connection, UUID tenant, UUID source, UUID event, String category) throws Exception {
        return update(connection, "insert into fuel_exception_case(id,tenant_id,category,lifecycle,impact,source_type,source_id,source_event_id,summary,safe_metadata,occurred_at,review_required,handoff_status,created_by,version,created_at,updated_at) values(?,?,?,'OPEN','MEDIUM',?,?,?,'Concurrent review','{}',?,true,'NOT_REQUIRED',?,0,?,?)",
                UUID.randomUUID(), tenant, category, category.equals("NEGATIVE_BUNKER_BALANCE") ? "REJECTED_BUNKER_COMMAND" : "FUEL_ISSUE",
                source, event, OffsetDateTime.now(), UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now());
    }

    private UUID seedCase(String lifecycle) {
        UUID tenant = UUID.randomUUID(), id = UUID.randomUUID(), nowActor = UUID.randomUUID();
        jdbc.sql("insert into fuel_exception_case(id,tenant_id,category,lifecycle,impact,source_type,source_id,summary,safe_metadata,occurred_at,review_required,handoff_status,created_by,version,created_at,updated_at) values(:id,:t,'SUSPECTED_FUEL_LOSS',:state,'MEDIUM','FUEL_ISSUE',:source,'Concurrent review','{}',:now,true,'NOT_REQUIRED',:actor,0,:now,:now)")
                .param("id", id).param("t", tenant).param("state", lifecycle).param("source", UUID.randomUUID())
                .param("actor", nowActor).param("now", OffsetDateTime.now()).update();
        return id;
    }

    private UUID seedCorrection(UUID exception, String status, String execution, long version) {
        UUID id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("insert into fuel_exception_correction(id,tenant_id,exception_id,correction_type,owner_command,changes_financial_fact,status,requested_by,version,created_at,updated_at,owner_module,owner_type,requested_command_type,safe_request_evidence,before_snapshot_hash,request_reason,requested_at,execution_state,idempotency_key) values(:id,:t,:x,'BUNKER_STOCK_ADJUSTMENT','{\"reason\":\"race\"}',true,:status,:actor,:version,:now,:now,'FUEL','BUNKER_STOCK_ADJUSTMENT','BUNKER_STOCK_ADJUSTMENT','{\"reason\":\"race\"}',repeat('a',64),'race',:now,:execution,:id)")
                .param("id", id).param("t", tenant(exception)).param("x", exception).param("status", status)
                .param("actor", UUID.randomUUID()).param("version", version).param("now", now).param("execution", execution).update();
        return id;
    }

    private int update(Connection connection, String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement.executeUpdate();
        }
    }

    private UUID tenant(UUID exception) { return jdbc.sql("select tenant_id from fuel_exception_case where id=:id").param("id", exception).query(UUID.class).single(); }
    private int count(String table, String column, UUID id) { return jdbc.sql("select count(*) from " + table + " where " + column + "=:id").param("id", id).query(Integer.class).single(); }
    private String status(String table, UUID id) { return jdbc.sql("select " + (table.endsWith("case") ? "lifecycle" : "status") + " from " + table + " where id=:id").param("id", id).query(String.class).single(); }
    private void assertCase(UUID id, String lifecycle, long version) { assertThat(jdbc.sql("select lifecycle||':'||version from fuel_exception_case where id=:id").param("id", id).query(String.class).single()).isEqualTo(lifecycle + ":" + version); }
    private static void assertOneWinner(List<Integer> results) { assertThat(results).containsExactlyInAnyOrder(1, 0); }

    @FunctionalInterface interface SqlWork { int run(Connection connection) throws Exception; }
}
