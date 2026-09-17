package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsEdgeCaseException;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEpisode;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsExceptionEvidence;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionEvidenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GpsExceptionRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.ArrayList;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class JdbcGpsExceptionRepositoryAdapter
        implements GpsExceptionRepositoryPort, GpsExceptionEvidenceRepositoryPort {
    private static final String SELECT = "SELECT * FROM tracking_gps_exception_episode ";
    private final JdbcTemplate jdbc;

    JdbcGpsExceptionRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<GpsExceptionEpisode> findActive(
            UUID tenantId, UUID deviceId, ExceptionType type) {
        return active(tenantId, deviceId, type, false);
    }

    @Override
    public Optional<GpsExceptionEpisode> findActiveForUpdate(
            UUID tenantId, UUID deviceId, ExceptionType type) {
        return active(tenantId, deviceId, type, true);
    }

    private Optional<GpsExceptionEpisode> active(
            UUID tenantId, UUID deviceId, ExceptionType type, boolean lock) {
        List<GpsExceptionEpisode> rows = jdbc.query(SELECT
                + "WHERE tenant_id=? AND tracking_device_id=? AND exception_type=? "
                + "AND status IN ('OPEN','ACKNOWLEDGED','RECOVERING')"
                + (lock ? " FOR UPDATE" : ""), this::mapEpisode,
                tenantId, deviceId, type.name());
        return rows.stream().findFirst();
    }

    @Override
    public List<GpsExceptionEpisode> findActiveByDeviceForUpdate(UUID tenantId, UUID deviceId) {
        return List.copyOf(jdbc.query(SELECT + "WHERE tenant_id=? AND tracking_device_id=? "
                + "AND status IN ('OPEN','ACKNOWLEDGED','RECOVERING') "
                + "ORDER BY exception_type FOR UPDATE", this::mapEpisode, tenantId, deviceId));
    }

    @Override
    public Optional<GpsExceptionEpisode> findById(UUID tenantId, UUID episodeId) {
        return jdbc.query(SELECT + "WHERE tenant_id=? AND id=?", this::mapEpisode,
                tenantId, episodeId).stream().findFirst();
    }

    @Override public Optional<GpsExceptionEpisode> findByIdForUpdate(UUID tenantId, UUID episodeId) {
        return jdbc.query(SELECT + "WHERE tenant_id=? AND id=? FOR UPDATE", this::mapEpisode,
                tenantId, episodeId).stream().findFirst();
    }

    @Override public List<GpsExceptionEpisode> search(UUID tenantId, Instant from, Instant to,
            EpisodeStatus status, ExceptionType type, Severity severity, UUID vehicleId, UUID deviceId,
            Instant afterTimestamp, UUID afterId, int limit) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE tenant_id=? AND last_observed_at>=? AND last_observed_at<? ");
        List<Object> args = new ArrayList<>(List.of(tenantId, Timestamp.from(from), Timestamp.from(to)));
        if (status != null) { sql.append("AND status=? "); args.add(status.name()); }
        if (type != null) { sql.append("AND exception_type=? "); args.add(type.name()); }
        if (severity != null) { sql.append("AND severity=? "); args.add(severity.name()); }
        if (vehicleId != null) { sql.append("AND vehicle_id=? "); args.add(vehicleId); }
        if (deviceId != null) { sql.append("AND tracking_device_id=? "); args.add(deviceId); }
        if (afterTimestamp != null) { sql.append("AND (last_observed_at<? OR (last_observed_at=? AND id<?)) ");
            args.add(Timestamp.from(afterTimestamp)); args.add(Timestamp.from(afterTimestamp)); args.add(afterId); }
        sql.append("ORDER BY last_observed_at DESC,id DESC LIMIT ?"); args.add(limit);
        return List.copyOf(jdbc.query(sql.toString(), this::mapEpisode, args.toArray()));
    }

    @Override
    public List<GpsExceptionEpisode> findByTenant(UUID tenantId, int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("GPS exception limit must be 1..500");
        }
        return List.copyOf(jdbc.query(SELECT + "WHERE tenant_id=? "
                + "ORDER BY last_observed_at DESC,id DESC LIMIT ?", this::mapEpisode,
                tenantId, limit));
    }

    @Override
    public GpsExceptionEpisode save(GpsExceptionEpisode episode) {
        if (episode.version() == 0) {
            try {
                jdbc.update("""
                        INSERT INTO tracking_gps_exception_episode(
                         id,tenant_id,tracking_device_id,vehicle_id,exception_type,severity,status,
                         opened_at,last_observed_at,resolved_at,evidence_count,
                         consecutive_recovery_points,version,acknowledgement_reason,created_at,updated_at)
                        VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,now(),now())
                        """, episode.id(), episode.tenantId(), episode.deviceId(), episode.vehicleId(),
                        episode.type().name(), episode.severity().name(), episode.status().name(),
                        Timestamp.from(episode.openedAt()), Timestamp.from(episode.lastObservedAt()),
                        timestamp(episode.resolvedAt()), episode.evidenceCount(),
                        episode.consecutiveRecoveryPoints(), episode.version(),
                        episode.acknowledgementReason());
                return episode;
            } catch (DuplicateKeyException exception) {
                throw new GpsEdgeCaseException("ACTIVE_EPISODE_CONFLICT",
                        "An active GPS exception already exists");
            }
        }
        int updated = jdbc.update("""
                UPDATE tracking_gps_exception_episode SET vehicle_id=?,severity=?,status=?,
                 last_observed_at=?,resolved_at=?,evidence_count=?,consecutive_recovery_points=?,
                 version=?,acknowledgement_reason=?,updated_at=now()
                WHERE tenant_id=? AND id=? AND version=?
                """, episode.vehicleId(), episode.severity().name(), episode.status().name(),
                Timestamp.from(episode.lastObservedAt()), timestamp(episode.resolvedAt()),
                episode.evidenceCount(), episode.consecutiveRecoveryPoints(), episode.version(),
                episode.acknowledgementReason(), episode.tenantId(), episode.id(), episode.version() - 1);
        if (updated != 1) {
            throw new GpsEdgeCaseException("OPTIMISTIC_CONFLICT", "GPS exception changed concurrently");
        }
        return episode;
    }

    @Override
    public boolean append(GpsExceptionEvidence evidence) {
        return jdbc.update("""
                INSERT INTO tracking_gps_exception_evidence(
                 id,tenant_id,episode_id,evidence_identity,telemetry_history_id,
                 telemetry_source_timestamp,assessed_at,trust,ordering_classification,
                 reliability_state,quality_codes,transition,created_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                """, evidence.id(), evidence.tenantId(), evidence.episodeId(),
                evidence.evidenceIdentity(), evidence.telemetryHistoryId(),
                timestamp(evidence.telemetrySourceTimestamp()), Timestamp.from(evidence.assessedAt()),
                evidence.trust().name(), evidence.ordering().name(), evidence.reliabilityState().name(),
                evidence.qualityCodes(), evidence.transition().name(), Timestamp.from(evidence.createdAt())) == 1;
    }

    @Override public List<GpsExceptionEvidence> findByEpisode(UUID tenantId, UUID episodeId,
            Instant afterTimestamp, UUID afterId, int limit) {
        String sql = "SELECT * FROM tracking_gps_exception_evidence WHERE tenant_id=? AND episode_id=? "
                + (afterTimestamp == null ? "" : "AND (assessed_at<? OR (assessed_at=? AND id<?)) ")
                + "ORDER BY assessed_at DESC,id DESC LIMIT ?";
        Object[] args = afterTimestamp == null ? new Object[]{tenantId,episodeId,limit}
                : new Object[]{tenantId,episodeId,Timestamp.from(afterTimestamp),Timestamp.from(afterTimestamp),afterId,limit};
        return List.copyOf(jdbc.query(sql, (row,n) -> new GpsExceptionEvidence(
                UUID.fromString(row.getString("id")), UUID.fromString(row.getString("tenant_id")),
                UUID.fromString(row.getString("episode_id")), row.getString("evidence_identity"),
                uuid(row.getString("telemetry_history_id")), instant(row.getTimestamp("telemetry_source_timestamp")),
                row.getTimestamp("assessed_at").toInstant(),
                com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Trust.valueOf(row.getString("trust")),
                com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Ordering.valueOf(row.getString("ordering_classification")),
                com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ReliabilityState.valueOf(row.getString("reliability_state")),
                row.getString("quality_codes"), GpsExceptionEvidence.Transition.valueOf(row.getString("transition")),
                row.getTimestamp("created_at").toInstant()), args));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private GpsExceptionEpisode mapEpisode(ResultSet row, int ignoredRowNumber) throws SQLException {
        return new GpsExceptionEpisode(UUID.fromString(row.getString("id")),
                UUID.fromString(row.getString("tenant_id")),
                UUID.fromString(row.getString("tracking_device_id")),
                uuid(row.getString("vehicle_id")), ExceptionType.valueOf(row.getString("exception_type")),
                Severity.valueOf(row.getString("severity")), EpisodeStatus.valueOf(row.getString("status")),
                row.getTimestamp("opened_at").toInstant(), row.getTimestamp("last_observed_at").toInstant(),
                instant(row.getTimestamp("resolved_at")), row.getLong("evidence_count"),
                row.getInt("consecutive_recovery_points"), row.getLong("version"),
                row.getString("acknowledgement_reason"));
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
