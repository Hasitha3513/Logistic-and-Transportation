package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryEvaluationDispatchPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
final class JdbcTelemetryEvaluationDispatchRepository implements TelemetryEvaluationDispatchPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcTelemetryEvaluationDispatchRepository(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override public void enqueue(HistoricalTelemetry telemetry) {
        for (Evaluator evaluator : List.of(Evaluator.GEOFENCE, Evaluator.SPEED,
                Evaluator.ROUTE_DEVIATION)) {
            insert(telemetry, evaluator);
        }
        if (idleEligible(telemetry)) {
            insert(telemetry, Evaluator.IDLE);
        }
    }

    private void insert(HistoricalTelemetry telemetry, Evaluator evaluator) {
            jdbc.update("""
                    INSERT INTO tracking_telemetry_evaluation_dispatch(
                     tenant_id,source_timestamp,history_id,dedupe_identity,vehicle_id,evaluator_type,
                     status,attempt_count,next_attempt_at,created_at,updated_at)
                    SELECT tenant_id,source_timestamp,id,dedupe_identity,vehicle_id,?,'PENDING',0,now(),now(),now()
                    FROM tracking_position_history
                    WHERE tenant_id=? AND source_timestamp=? AND id=? AND dedupe_identity=? AND vehicle_id=?
                    ON CONFLICT DO NOTHING
                    """, evaluator.name(), telemetry.tenantId(), Timestamp.from(telemetry.recordedAt()),
                    telemetry.eventId(), telemetry.dedupeIdentity(), telemetry.vehicleId());
    }

    private boolean idleEligible(HistoricalTelemetry telemetry) {
        if (telemetry.eventVersion() != 3 || telemetry.engineRunningState() == null
                || telemetry.engineRunningSource() == null) {
            return false;
        }
        Boolean supported = jdbc.query("""
                SELECT true FROM tracking_device_telemetry_capability
                WHERE tenant_id=? AND tracking_device_id=? AND capability='ENGINE_RUNNING'
                  AND capability_state='SUPPORTED' AND effective_from<=?
                  AND (effective_to IS NULL OR effective_to>?)
                ORDER BY effective_from DESC,id DESC LIMIT 1
                """, (row, number) -> row.getBoolean(1), telemetry.tenantId(), telemetry.deviceId(),
                ts(telemetry.recordedAt()), ts(telemetry.recordedAt())).stream().findFirst().orElse(false);
        return supported;
    }

    @Override public List<Dispatch> claim(String owner, Instant now, Instant leaseUntil, int limit) {
        return claim(owner, now, leaseUntil, limit, false);
    }

    @Override public List<Dispatch> claimIdle(String owner, Instant now, Instant leaseUntil, int limit) {
        return claim(owner, now, leaseUntil, limit, true);
    }

    private List<Dispatch> claim(String owner, Instant now, Instant leaseUntil, int limit,
            boolean idleOnly) {
        requireOwner(owner);
        if (limit < 1 || limit > 100 || !leaseUntil.isAfter(now)) throw new IllegalArgumentException("Invalid claim");
        String evaluatorFilter = idleOnly ? "evaluator_type='IDLE'" : "evaluator_type<>'IDLE'";
        String sql = """
                WITH due AS (
                 SELECT dispatch_id FROM tracking_telemetry_evaluation_dispatch
                 WHERE %s AND next_attempt_at<=? AND (status IN('PENDING','FAILED')
                   OR (status='PROCESSING' AND lease_until<=?))
                 ORDER BY next_attempt_at,tenant_id,vehicle_id,source_timestamp,evaluator_type,dispatch_id
                 FOR UPDATE SKIP LOCKED LIMIT ?)
                UPDATE tracking_telemetry_evaluation_dispatch d
                SET status='PROCESSING',attempt_count=attempt_count+1,lease_owner=?,lease_until=?,
                    completed_at=NULL,last_error_code=NULL,updated_at=?,version=version+1
                FROM due WHERE d.dispatch_id=due.dispatch_id RETURNING d.*
                """.replace("%s", evaluatorFilter);
        return transactions.execute(status -> List.copyOf(jdbc.query(sql, this::map,
                ts(now), ts(now), limit, owner, ts(leaseUntil), ts(now))));
    }

    @Override public void complete(UUID id, String owner, Instant now) {
        changed(jdbc.update("""
                UPDATE tracking_telemetry_evaluation_dispatch
                SET status='COMPLETED',lease_owner=NULL,lease_until=NULL,completed_at=?,updated_at=?,version=version+1
                WHERE dispatch_id=? AND status='PROCESSING' AND lease_owner=? AND lease_until>?
                """, ts(now), ts(now), id, owner, ts(now)));
    }

    @Override public void retry(UUID id, String owner, Instant now, Instant next, String code) {
        changed(jdbc.update("""
                UPDATE tracking_telemetry_evaluation_dispatch
                SET status='PENDING',next_attempt_at=?,lease_owner=NULL,lease_until=NULL,last_error_code=?,updated_at=?,version=version+1
                WHERE dispatch_id=? AND status='PROCESSING' AND lease_owner=? AND lease_until>?
                """, ts(next), safe(code), ts(now), id, owner, ts(now)));
    }

    @Override public void fail(UUID id, String owner, Instant now, String code) {
        changed(jdbc.update("""
                UPDATE tracking_telemetry_evaluation_dispatch
                SET status='FAILED',next_attempt_at=?,lease_owner=NULL,lease_until=NULL,last_error_code=?,updated_at=?,version=version+1
                WHERE dispatch_id=? AND status='PROCESSING' AND lease_owner=? AND lease_until>?
                """, ts(now.plusSeconds(300)), safe(code), ts(now), id, owner, ts(now)));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Dispatch map(ResultSet row, int number) throws SQLException {
        return new Dispatch(row.getObject("dispatch_id", UUID.class), row.getObject("tenant_id", UUID.class),
                row.getTimestamp("source_timestamp").toInstant(), row.getObject("history_id", UUID.class),
                row.getString("dedupe_identity"), row.getObject("vehicle_id", UUID.class),
                Evaluator.valueOf(row.getString("evaluator_type")), row.getInt("attempt_count"));
    }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private static String safe(String code) {
        return code != null && code.matches("[A-Z0-9_]{1,120}") ? code : "EVALUATION_FAILED";
    }
    private static void requireOwner(String owner) {
        if (owner == null || !owner.matches("[A-Za-z0-9._:-]{1,120}")) throw new IllegalArgumentException("Invalid owner");
    }
    private static void changed(int count) { if (count != 1) throw new IllegalStateException("Dispatch lease lost"); }
}
