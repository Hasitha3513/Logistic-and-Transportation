package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Episode;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Evidence;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Mutation;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.PersistResult;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.State;
import com.transportlogistics.app.tracking.ports.outbound.IdlePersistencePort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
final class JdbcIdlePersistenceAdapter implements IdlePersistencePort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcIdlePersistenceAdapter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public Optional<State> findState(UUID tenantId, UUID vehicleId) {
        return jdbc.query("SELECT * FROM tracking_idle_state WHERE tenant_id=? AND vehicle_id=?",
                this::mapState, tenantId, vehicleId).stream().findFirst();
    }

    @Override
    public Optional<Episode> findEpisode(UUID tenantId, UUID episodeId) {
        return jdbc.query("SELECT * FROM tracking_idle_episode WHERE tenant_id=? AND id=?",
                this::mapEpisode, tenantId, episodeId).stream().findFirst();
    }

    @Override
    public PersistResult persist(Mutation mutation) {
        return transactions.execute(status -> persistAtomically(mutation));
    }

    private PersistResult persistAtomically(Mutation mutation) {
        State state = mutation.state();
        lock(state.tenantId(), state.vehicleId());
        Evidence evidence = mutation.evidence();
        Integer duplicate = jdbc.queryForObject("""
                SELECT count(*) FROM tracking_idle_episode_evidence
                WHERE tenant_id=? AND episode_id=? AND source_timestamp=? AND dedupe_identity=?
                """, Integer.class, evidence.tenantId(), evidence.episodeId(),
                timestamp(evidence.sourceTimestamp()), evidence.dedupeIdentity());
        if (duplicate != null && duplicate > 0) {
            return PersistResult.DUPLICATE;
        }
        persistEpisode(mutation);
        insertEvidence(evidence);
        persistState(mutation);
        return PersistResult.APPLIED;
    }

    private void lock(UUID tenantId, UUID vehicleId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(? || ':' || ?, 0))",
                row -> { }, tenantId.toString(), vehicleId.toString());
    }

    private void persistEpisode(Mutation mutation) {
        Episode episode = mutation.episode();
        if (mutation.createEpisode()) {
            int changed = jdbc.update("""
                    INSERT INTO tracking_idle_episode(
                     id,tenant_id,vehicle_id,device_id,lifecycle,start_source_timestamp,confirmed_at,
                     last_source_timestamp,end_source_timestamp,end_reason,credited_seconds,evidence_count,
                     version,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,now(),now())
                    """, episode.id(), episode.tenantId(), episode.vehicleId(), episode.deviceId(),
                    episode.lifecycle().name(), timestamp(episode.startSourceTimestamp()),
                    timestamp(episode.confirmedAt()), timestamp(episode.lastSourceTimestamp()),
                    timestamp(episode.endSourceTimestamp()), name(episode.endReason()),
                    episode.creditedSeconds(), episode.evidenceCount(), episode.version());
            changed(changed, "Idle episode create conflict");
            return;
        }
        int changed = jdbc.update("""
                UPDATE tracking_idle_episode SET lifecycle=?,confirmed_at=?,last_source_timestamp=?,
                 end_source_timestamp=?,end_reason=?,credited_seconds=?,evidence_count=?,
                 version=version+1,updated_at=now()
                WHERE tenant_id=? AND id=? AND vehicle_id=? AND version=?
                """, episode.lifecycle().name(), timestamp(episode.confirmedAt()),
                timestamp(episode.lastSourceTimestamp()), timestamp(episode.endSourceTimestamp()),
                name(episode.endReason()), episode.creditedSeconds(), episode.evidenceCount(),
                episode.tenantId(), episode.id(), episode.vehicleId(), mutation.expectedEpisodeVersion());
        changed(changed, "Idle episode optimistic conflict");
    }

    private void insertEvidence(Evidence evidence) {
        int changed = jdbc.update("""
                INSERT INTO tracking_idle_episode_evidence(
                 id,tenant_id,episode_id,vehicle_id,device_id,history_id,source_timestamp,
                 dedupe_identity,outcome,engine_running_state,engine_running_source,speed_kph,
                 horizontal_accuracy_meters,adjusted_distance_meters,credited_delta_seconds)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, evidence.id(), evidence.tenantId(), evidence.episodeId(), evidence.vehicleId(),
                evidence.deviceId(), evidence.historyId(), timestamp(evidence.sourceTimestamp()),
                evidence.dedupeIdentity(), evidence.outcome().name(), evidence.engineRunningState(),
                evidence.engineRunningSource(), evidence.speedKph(), evidence.horizontalAccuracyMeters(),
                evidence.adjustedDistanceMeters(), evidence.creditedDeltaSeconds());
        changed(changed, "Idle evidence insert conflict");
    }

    private void persistState(Mutation mutation) {
        State state = mutation.state();
        if (mutation.expectedStateVersion() < 0) {
            int changed = jdbc.update("""
                    INSERT INTO tracking_idle_state(
                     tenant_id,vehicle_id,device_id,state,capability_state,latest_source_timestamp,
                     candidate_started_at,last_qualifying_at,credited_seconds,evidence_count,
                     open_episode_id,reference_history_id,recovery_started_at,last_dedupe_identity,
                     version,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now(),now())
                    """, state.tenantId(), state.vehicleId(), state.deviceId(), state.state().name(),
                    state.capabilityState().name(), timestamp(state.latestSourceTimestamp()),
                    timestamp(state.candidateStartedAt()), timestamp(state.lastQualifyingAt()),
                    state.creditedSeconds(), state.evidenceCount(), state.openEpisodeId(),
                    state.referenceHistoryId(), timestamp(state.recoveryStartedAt()),
                    state.lastDedupeIdentity(), state.version());
            changed(changed, "Idle state create conflict");
            return;
        }
        int changed = jdbc.update("""
                UPDATE tracking_idle_state SET device_id=?,state=?,capability_state=?,
                 latest_source_timestamp=?,candidate_started_at=?,last_qualifying_at=?,
                 credited_seconds=?,evidence_count=?,open_episode_id=?,reference_history_id=?,
                 recovery_started_at=?,last_dedupe_identity=?,
                 version=version+1,updated_at=now()
                WHERE tenant_id=? AND vehicle_id=? AND version=?
                """, state.deviceId(), state.state().name(), state.capabilityState().name(),
                timestamp(state.latestSourceTimestamp()), timestamp(state.candidateStartedAt()),
                timestamp(state.lastQualifyingAt()), state.creditedSeconds(), state.evidenceCount(),
                state.openEpisodeId(), state.referenceHistoryId(), timestamp(state.recoveryStartedAt()),
                state.lastDedupeIdentity(), state.tenantId(), state.vehicleId(),
                mutation.expectedStateVersion());
        changed(changed, "Idle state optimistic conflict");
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private State mapState(ResultSet row, int number) throws SQLException {
        return new State(row.getObject("tenant_id", UUID.class),
                row.getObject("vehicle_id", UUID.class), row.getObject("device_id", UUID.class),
                com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.StateValue
                        .valueOf(row.getString("state")),
                com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.CapabilityState
                        .valueOf(row.getString("capability_state")),
                row.getTimestamp("latest_source_timestamp").toInstant(),
                instant(row, "candidate_started_at"), instant(row, "last_qualifying_at"),
                row.getLong("credited_seconds"), row.getInt("evidence_count"),
                row.getObject("open_episode_id", UUID.class),
                row.getObject("reference_history_id", UUID.class), instant(row, "recovery_started_at"),
                row.getString("last_dedupe_identity"),
                row.getLong("version"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Episode mapEpisode(ResultSet row, int number) throws SQLException {
        String endReason = row.getString("end_reason");
        return new Episode(row.getObject("id", UUID.class), row.getObject("tenant_id", UUID.class),
                row.getObject("vehicle_id", UUID.class), row.getObject("device_id", UUID.class),
                com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.EpisodeLifecycle
                        .valueOf(row.getString("lifecycle")),
                row.getTimestamp("start_source_timestamp").toInstant(), instant(row, "confirmed_at"),
                row.getTimestamp("last_source_timestamp").toInstant(), instant(row, "end_source_timestamp"),
                endReason == null ? null
                        : com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.EndReason
                                .valueOf(endReason),
                row.getLong("credited_seconds"), row.getInt("evidence_count"), row.getLong("version"));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static void changed(int count, String message) {
        if (count != 1) {
            throw new OptimisticLockingFailureException(message);
        }
    }
}
