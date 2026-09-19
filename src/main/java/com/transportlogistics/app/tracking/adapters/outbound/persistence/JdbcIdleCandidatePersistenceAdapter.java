package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.idle.IdleCandidate;
import com.transportlogistics.app.tracking.domain.idle.IdleCandidate.Evidence;
import com.transportlogistics.app.tracking.ports.outbound.IdleCandidatePersistencePort;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name="app.tracking.hybrid-storage.enabled",havingValue="true")
final class JdbcIdleCandidatePersistenceAdapter implements IdleCandidatePersistencePort {
    private static final Duration RETENTION=Duration.ofDays(180);
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions;
    JdbcIdleCandidatePersistenceAdapter(JdbcTemplate jdbc,TransactionTemplate transactions){this.jdbc=jdbc;this.transactions=transactions;}

    @Override public Optional<IdleCandidate> find(UUID tenantId,UUID vehicleId){
        return jdbc.query("SELECT * FROM tracking_idle_state WHERE tenant_id=? AND vehicle_id=? AND state='CANDIDATE' AND open_episode_id IS NULL",
                (r,n)->new IdleCandidate(tenantId,vehicleId,r.getObject("device_id",UUID.class),r.getObject("candidate_id",UUID.class),
                        r.getObject("reference_history_id",UUID.class),r.getTimestamp("candidate_started_at").toInstant(),
                        r.getTimestamp("latest_source_timestamp").toInstant(),instant(r.getTimestamp("last_qualifying_at")),
                        instant(r.getTimestamp("recovery_started_at")),r.getLong("credited_seconds"),r.getInt("evidence_count"),
                        r.getString("last_dedupe_identity"),r.getLong("version")),tenantId,vehicleId).stream().findFirst();
    }
    @Override public Result save(IdleCandidate c,Evidence e,long expected){return transactions.execute(s->{lock(c.tenantId(),c.vehicleId());
        if(exists(e))return Result.DUPLICATE; insertEvidence(e); int changed;
        if(expected<0) changed=jdbc.update("INSERT INTO tracking_idle_state(tenant_id,vehicle_id,device_id,state,capability_state,latest_source_timestamp,candidate_started_at,last_qualifying_at,credited_seconds,evidence_count,open_episode_id,last_dedupe_identity,version,candidate_id,reference_history_id,recovery_started_at) VALUES(?,?,?,'CANDIDATE','SUPPORTED',?,?,?,?,?,NULL,?,0,?,?,?)",c.tenantId(),c.vehicleId(),c.deviceId(),ts(c.latestSourceTimestamp()),ts(c.startedAt()),ts(c.lastQualifyingAt()),c.creditedSeconds(),c.evidenceCount(),c.lastDedupeIdentity(),c.candidateId(),c.referenceHistoryId(),ts(c.recoveryStartedAt()));
        else changed=jdbc.update("UPDATE tracking_idle_state SET device_id=?,latest_source_timestamp=?,last_qualifying_at=?,credited_seconds=?,evidence_count=?,last_dedupe_identity=?,reference_history_id=?,recovery_started_at=?,version=version+1 WHERE tenant_id=? AND vehicle_id=? AND candidate_id=? AND version=?",c.deviceId(),ts(c.latestSourceTimestamp()),ts(c.lastQualifyingAt()),c.creditedSeconds(),c.evidenceCount(),c.lastDedupeIdentity(),c.referenceHistoryId(),ts(c.recoveryStartedAt()),c.tenantId(),c.vehicleId(),c.candidateId(),expected);
        changed(changed);return Result.APPLIED;});}
    @Override public void discard(UUID tenant,UUID vehicle,UUID candidate,long expected){transactions.executeWithoutResult(s->{lock(tenant,vehicle);changed(jdbc.update("DELETE FROM tracking_idle_state WHERE tenant_id=? AND vehicle_id=? AND candidate_id=? AND version=?",tenant,vehicle,candidate,expected));});}
    @Override public UUID promote(IdleCandidate c,Evidence e,Instant confirmed,long expected){return transactions.execute(s->{lock(c.tenantId(),c.vehicleId());if(!exists(e))insertEvidence(e);UUID episode=UUID.randomUUID();jdbc.update("INSERT INTO tracking_idle_episode(id,tenant_id,vehicle_id,device_id,lifecycle,start_source_timestamp,confirmed_at,last_source_timestamp,credited_seconds,evidence_count,candidate_id) VALUES(?,?,?,?,'CONFIRMED',?,?,?,?,?,?)",episode,c.tenantId(),c.vehicleId(),c.deviceId(),ts(c.startedAt()),ts(confirmed),ts(c.latestSourceTimestamp()),c.creditedSeconds(),c.evidenceCount(),c.candidateId());changed(jdbc.update("UPDATE tracking_idle_state SET state='IDLE',candidate_id=NULL,recovery_started_at=NULL,open_episode_id=?,version=version+1 WHERE tenant_id=? AND vehicle_id=? AND candidate_id=? AND version=?",episode,c.tenantId(),c.vehicleId(),c.candidateId(),expected));return episode;});}
    @Override public int purgeExpired(Instant now,int limit){if(limit<1||limit>1000)throw new IllegalArgumentException("Invalid purge limit");return jdbc.update("DELETE FROM tracking_idle_candidate_evidence WHERE id IN (SELECT id FROM tracking_idle_candidate_evidence WHERE retain_until<=? ORDER BY retain_until,id LIMIT ?)",ts(now),limit);}
    private void insertEvidence(Evidence e){jdbc.update("INSERT INTO tracking_idle_candidate_evidence(id,tenant_id,candidate_id,vehicle_id,device_id,history_id,source_timestamp,dedupe_identity,outcome,engine_running_state,engine_running_source,speed_kph,horizontal_accuracy_meters,adjusted_distance_meters,credited_delta_seconds,retain_until) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",e.id(),e.tenantId(),e.candidateId(),e.vehicleId(),e.deviceId(),e.historyId(),ts(e.sourceTimestamp()),e.dedupeIdentity(),e.outcome(),e.engineRunningState(),e.engineRunningSource(),e.speedKph(),e.accuracyMeters(),e.adjustedDistanceMeters(),e.creditedDeltaSeconds(),ts(e.sourceTimestamp().plus(RETENTION)));}
    private boolean exists(Evidence e){Integer count=jdbc.queryForObject("SELECT count(*) FROM tracking_idle_candidate_evidence WHERE tenant_id=? AND candidate_id=? AND source_timestamp=? AND dedupe_identity=?",Integer.class,e.tenantId(),e.candidateId(),ts(e.sourceTimestamp()),e.dedupeIdentity());return count!=null&&count>0;}
    private void lock(UUID t,UUID v){jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(? || ':' || ?,0))",r->{},t.toString(),v.toString());}
    private static Timestamp ts(Instant i){return i==null?null:Timestamp.from(i);} private static Instant instant(Timestamp t){return t==null?null:t.toInstant();}
    private static void changed(int n){if(n!=1)throw new OptimisticLockingFailureException("Idle candidate optimistic conflict");}
}
