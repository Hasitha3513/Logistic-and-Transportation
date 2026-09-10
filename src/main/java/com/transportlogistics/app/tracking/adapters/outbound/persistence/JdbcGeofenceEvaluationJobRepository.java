package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceEvaluationJob;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceEvaluationJobRepositoryPort;
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
class JdbcGeofenceEvaluationJobRepository implements GeofenceEvaluationJobRepositoryPort {
    private static final int MAX_CLAIM = 100;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcGeofenceEvaluationJobRepository(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public GeofenceEvaluationJob enqueue(UUID tenantId, UUID positionId, Instant now) {
        jdbc.update("""
                INSERT INTO tracking_geofence_evaluation_job(
                 tenant_id,position_id,status,attempt,next_attempt_at,created_at,updated_at)
                VALUES(?,?,'PENDING',0,?,?,?) ON CONFLICT(tenant_id,position_id) DO NOTHING
                """, tenantId, positionId, timestamp(now), timestamp(now), timestamp(now));
        return find(tenantId, positionId).orElseThrow();
    }

    @Override
    public Optional<GeofenceEvaluationJob> find(UUID tenantId, UUID positionId) {
        return jdbc.query("""
                SELECT * FROM tracking_geofence_evaluation_job
                WHERE tenant_id=? AND position_id=?
                """, this::map, tenantId, positionId).stream().findFirst();
    }

    @Override
    public List<GeofenceEvaluationJob> claimDue(
            String leaseOwner, Instant now, Instant leaseUntil, int limit) {
        requireOwnerAndLease(leaseOwner, now, leaseUntil);
        if (limit < 1 || limit > MAX_CLAIM) {
            throw new IllegalArgumentException("Job claim limit must be between 1 and 100");
        }
        return transactions.execute(status -> List.copyOf(jdbc.query("""
                WITH due AS (
                 SELECT tenant_id,position_id FROM tracking_geofence_evaluation_job
                 WHERE next_attempt_at<=? AND (
                  status IN('PENDING','FAILED')
                  OR (status='PROCESSING' AND lease_until<=?))
                 ORDER BY next_attempt_at,tenant_id,position_id
                 FOR UPDATE SKIP LOCKED LIMIT ?
                )
                UPDATE tracking_geofence_evaluation_job job
                SET status='PROCESSING',attempt=attempt+1,lease_owner=?,lease_until=?,updated_at=?
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
                UPDATE tracking_geofence_evaluation_job SET lease_until=?,updated_at=?
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING'
                  AND lease_owner=? AND lease_until>?
                """, timestamp(leaseUntil), timestamp(now), tenantId, positionId,
                leaseOwner, timestamp(now)) == 1;
    }

    @Override
    public boolean release(UUID tenantId, UUID positionId, String leaseOwner, Instant now) {
        requireOwner(leaseOwner);
        return jdbc.update("""
                UPDATE tracking_geofence_evaluation_job
                SET status='PENDING',lease_owner=NULL,lease_until=NULL,updated_at=?
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING' AND lease_owner=?
                """, timestamp(now), tenantId, positionId, leaseOwner) == 1;
    }

    @Override
    public void complete(UUID tenantId, UUID positionId, String leaseOwner, Instant completedAt) {
        requireOwner(leaseOwner);
        int changed = jdbc.update("""
                UPDATE tracking_geofence_evaluation_job
                SET status='COMPLETED',lease_owner=NULL,lease_until=NULL,updated_at=?
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING' AND lease_owner=?
                """, timestamp(completedAt), tenantId, positionId, leaseOwner);
        requireChanged(changed);
    }

    @Override
    public void retry(UUID tenantId, UUID positionId, String leaseOwner, Instant nextAttemptAt) {
        requireOwner(leaseOwner);
        int changed = jdbc.update("""
                UPDATE tracking_geofence_evaluation_job
                SET status='PENDING',next_attempt_at=?,lease_owner=NULL,lease_until=NULL,updated_at=now()
                WHERE tenant_id=? AND position_id=? AND status='PROCESSING' AND lease_owner=?
                """, timestamp(nextAttemptAt), tenantId, positionId, leaseOwner);
        requireChanged(changed);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private GeofenceEvaluationJob map(ResultSet row, int rowNumber) throws SQLException {
        return new GeofenceEvaluationJob(uuid(row, "tenant_id"), uuid(row, "position_id"),
                GeofenceEvaluationJob.Status.valueOf(row.getString("status")), row.getInt("attempt"),
                row.getString("lease_owner"), instant(row, "lease_until"), instant(row, "created_at"),
                instant(row, "next_attempt_at"));
    }

    private static void requireChanged(int changed) {
        if (changed != 1) {
            throw new BusinessRuleException("GEOFENCE_JOB_STALE_LEASE",
                    "Evaluation job lease is no longer owned by this worker");
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

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static UUID uuid(ResultSet row, String column) throws SQLException {
        return UUID.fromString(row.getString(column));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
