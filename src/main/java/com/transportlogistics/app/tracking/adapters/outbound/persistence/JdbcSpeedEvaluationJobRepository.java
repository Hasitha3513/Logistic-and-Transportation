package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import com.transportlogistics.app.tracking.ports.outbound.SpeedEvaluationJobRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class JdbcSpeedEvaluationJobRepository implements SpeedEvaluationJobRepositoryPort {
    private static final int MAX_CLAIM = 100;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcSpeedEvaluationJobRepository(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public SpeedEvaluationJob enqueue(UUID tenantId, UUID positionId, UUID vehicleId,
                                      Instant sourceTimestamp, Instant now) {
        jdbc.update("""
                INSERT INTO tracking_speed_evaluation_job(
                 tenant_id,position_id,vehicle_id,source_timestamp,status,attempt_count,
                 next_attempt_at,created_at,updated_at)
                SELECT tenant_id,id,vehicle_id,source_timestamp,'PENDING',0,?,?,?
                FROM tracking_position
                WHERE tenant_id=? AND id=? AND vehicle_id=? AND source_timestamp=?
                ON CONFLICT(tenant_id,position_id) DO NOTHING
                """, timestamp(now), timestamp(now), timestamp(now), tenantId, positionId,
                vehicleId, timestamp(sourceTimestamp));
        return find(tenantId, positionId).orElseThrow(() ->
                new BusinessRuleException("SPEED_JOB_POSITION_INVALID",
                        "Speed evaluation job requires the matching Tenant position"));
    }

    @Override
    public Optional<SpeedEvaluationJob> find(UUID tenantId, UUID positionId) {
        return jdbc.query("""
                SELECT * FROM tracking_speed_evaluation_job WHERE tenant_id=? AND position_id=?
                """, this::map, tenantId, positionId).stream().findFirst();
    }

    @Override
    public List<SpeedEvaluationJob> claimDue(String leaseOwner, Instant now,
                                              Instant leaseUntil, int limit) {
        requireOwnerAndLease(leaseOwner, now, leaseUntil);
        if (limit < 1 || limit > MAX_CLAIM) {
            throw new IllegalArgumentException("Job claim limit must be between 1 and 100");
        }
        return transactions.execute(transaction -> List.copyOf(jdbc.query("""
                WITH due AS (
                 SELECT tenant_id,position_id FROM tracking_speed_evaluation_job
                 WHERE next_attempt_at<=? AND (
                  status IN('PENDING','FAILED') OR (status='PROCESSING' AND lease_until<=?))
                 ORDER BY next_attempt_at,tenant_id,position_id
                 FOR UPDATE SKIP LOCKED LIMIT ?
                )
                UPDATE tracking_speed_evaluation_job job
                SET status='PROCESSING',attempt_count=attempt_count+1,lease_owner=?,lease_until=?,
                    last_error_code=NULL,updated_at=?
                FROM due WHERE job.tenant_id=due.tenant_id AND job.position_id=due.position_id
                RETURNING job.*
                """, this::map, timestamp(now), timestamp(now), limit, leaseOwner,
                timestamp(leaseUntil), timestamp(now))));
    }

    @Override
    public boolean renew(UUID tenantId, UUID positionId, String leaseOwner,
                         Instant now, Instant leaseUntil) {
        requireOwnerAndLease(leaseOwner, now, leaseUntil);
        return jdbc.update("""
                UPDATE tracking_speed_evaluation_job SET lease_until=?,updated_at=?
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING'
                  AND lease_owner=? AND lease_until>?
                """, timestamp(leaseUntil), timestamp(now), tenantId, positionId,
                leaseOwner, timestamp(now)) == 1;
    }

    @Override
    public boolean release(UUID tenantId, UUID positionId, String leaseOwner, Instant now) {
        requireOwner(leaseOwner);
        return jdbc.update("""
                UPDATE tracking_speed_evaluation_job
                SET status='PENDING',lease_owner=NULL,lease_until=NULL,updated_at=?
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING'
                  AND lease_owner=? AND lease_until>?
                """, timestamp(now), tenantId, positionId, leaseOwner, timestamp(now)) == 1;
    }

    @Override
    public void complete(UUID tenantId, UUID positionId, String leaseOwner, Instant completedAt) {
        requireOwner(leaseOwner);
        requireChanged(jdbc.update("""
                UPDATE tracking_speed_evaluation_job
                SET status='COMPLETED',lease_owner=NULL,lease_until=NULL,updated_at=?
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING'
                  AND lease_owner=? AND lease_until>?
                """, timestamp(completedAt), tenantId, positionId, leaseOwner, timestamp(completedAt)));
    }

    @Override
    public void retry(UUID tenantId, UUID positionId, String leaseOwner,
                      Instant now, Instant nextAttemptAt) {
        requireOwner(leaseOwner);
        requireChanged(jdbc.update("""
                UPDATE tracking_speed_evaluation_job
                SET status='PENDING',next_attempt_at=?,lease_owner=NULL,lease_until=NULL,updated_at=?
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING'
                  AND lease_owner=? AND lease_until>?
                """, timestamp(nextAttemptAt), timestamp(now), tenantId, positionId,
                leaseOwner, timestamp(now)));
    }

    @Override
    public void fail(UUID tenantId, UUID positionId, String leaseOwner,
                     String errorCode, Instant failedAt) {
        requireOwner(leaseOwner);
        if (errorCode == null || errorCode.isBlank() || errorCode.length() > 120) {
            throw new IllegalArgumentException("Job error code is invalid");
        }
        requireChanged(jdbc.update("""
                UPDATE tracking_speed_evaluation_job
                SET status='FAILED',next_attempt_at='infinity',lease_owner=NULL,lease_until=NULL,
                    last_error_code=?,updated_at=?
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING'
                  AND lease_owner=? AND lease_until>?
                """, errorCode, timestamp(failedAt), tenantId, positionId,
                leaseOwner, timestamp(failedAt)));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private SpeedEvaluationJob map(ResultSet row, int rowNumber) throws SQLException {
        return new SpeedEvaluationJob(uuid(row, "tenant_id"), uuid(row, "position_id"),
                uuid(row, "vehicle_id"), instant(row, "source_timestamp"),
                SpeedEvaluationJob.Status.valueOf(row.getString("status")),
                row.getInt("attempt_count"), instant(row, "next_attempt_at"),
                row.getString("lease_owner"), instant(row, "lease_until"),
                row.getString("last_error_code"), instant(row, "created_at"));
    }

    private static void requireChanged(int changed) {
        if (changed != 1) {
            throw new BusinessRuleException("SPEED_JOB_STALE_LEASE",
                    "Speed evaluation job lease is no longer owned by this worker");
        }
    }

    private static void requireOwnerAndLease(String owner, Instant now, Instant leaseUntil) {
        requireOwner(owner);
        if (now == null || leaseUntil == null || !leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("Job lease interval is invalid");
        }
    }

    private static void requireOwner(String owner) {
        if (owner == null || owner.isBlank() || owner.length() > 120) {
            throw new IllegalArgumentException("Job lease owner is invalid");
        }
    }

    private static Timestamp timestamp(Instant value) { return Timestamp.from(value); }
    private static UUID uuid(ResultSet row, String column) throws SQLException {
        return row.getObject(column, UUID.class);
    }
    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
