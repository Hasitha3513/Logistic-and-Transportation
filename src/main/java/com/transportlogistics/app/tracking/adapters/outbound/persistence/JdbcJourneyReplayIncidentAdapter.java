package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.CursorState;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.IncidentOverlay;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.OverlayType;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayQuery;
import com.transportlogistics.app.tracking.domain.journeyreplay.ReplayQueryPolicy;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayIncidentPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
@SuppressWarnings("PMD.UnusedFormalParameter")
final class JdbcJourneyReplayIncidentAdapter implements JourneyReplayIncidentPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate readOnly;

    JdbcJourneyReplayIncidentAdapter(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.readOnly = new TransactionTemplate(manager);
        this.readOnly.setReadOnly(true);
        this.readOnly.setTimeout(5);
    }

    @Override
    public List<IncidentOverlay> query(ReplayQuery query, CursorState cursor) {
        UUID tenantId = query.tenant().tenantId();
        UUID vehicleId = query.selector().id();
        Instant from = query.effectiveRange().from();
        Instant to = query.effectiveRange().to();
        return readOnly.execute(status -> queryInTransaction(query, cursor, tenantId, vehicleId, from, to));
    }

    private List<IncidentOverlay> queryInTransaction(
            ReplayQuery query, CursorState cursor, UUID tenantId, UUID vehicleId, Instant from, Instant to) {
        jdbc.execute("SET LOCAL statement_timeout = '5s'");
        List<IncidentOverlay> result = new ArrayList<>();
        if (query.overlays().contains(OverlayType.GEOFENCE)) {
            result.addAll(jdbc.query("""
                    SELECT id,transition,source_timestamp,severity
                    FROM tracking_geofence_transition
                    WHERE tenant_id=? AND vehicle_id=? AND source_timestamp>=? AND source_timestamp<?
                    """, this::geofence, tenantId, vehicleId, timestamp(from), timestamp(to)));
        }
        if (query.overlays().contains(OverlayType.SPEED)) {
            result.addAll(jdbc.query("""
                    SELECT id,start_source_timestamp,end_source_timestamp,severity,trip_id,route_id,route_version
                    FROM tracking_speed_episode
                    WHERE tenant_id=? AND vehicle_id=? AND start_source_timestamp<?
                      AND COALESCE(end_source_timestamp,?)>=?
                    """, this::speed, tenantId, vehicleId, timestamp(to), timestamp(to), timestamp(from)));
        }
        if (query.overlays().contains(OverlayType.ROUTE_DEVIATION)) {
            result.addAll(jdbc.query("""
                    SELECT id,start_source_timestamp,end_source_timestamp,severity,review_status,
                           trip_id,route_id,route_version
                    FROM tracking_route_deviation_episode
                    WHERE tenant_id=? AND vehicle_id=? AND start_source_timestamp<?
                      AND COALESCE(end_source_timestamp,?)>=?
                    """, this::routeDeviation, tenantId, vehicleId,
                    timestamp(to), timestamp(to), timestamp(from)));
            result.addAll(jdbc.query("""
                    SELECT review.id,review.reviewed_at,review.status,episode.severity,
                           episode.trip_id,episode.route_id,episode.route_version
                    FROM tracking_route_deviation_review review
                    JOIN tracking_route_deviation_episode episode
                      ON episode.tenant_id=review.tenant_id AND episode.id=review.episode_id
                    WHERE review.tenant_id=? AND episode.vehicle_id=?
                      AND review.reviewed_at>=? AND review.reviewed_at<?
                    """, this::routeDeviationReview, tenantId, vehicleId,
                    timestamp(from), timestamp(to)));
        }
        return result.stream()
                .sorted(Comparator.comparing(IncidentOverlay::sourceTimestamp)
                        .thenComparing(IncidentOverlay::evidenceId))
                .filter(item -> after(item, cursor))
                .limit((long) query.limit() + 1)
                .toList();
    }

    private static boolean after(IncidentOverlay item, CursorState cursor) {
        if (cursor == null) return true;
        int timestampOrder = item.sourceTimestamp().compareTo(cursor.position().sourceTimestamp());
        return timestampOrder > 0 || timestampOrder == 0
                && item.evidenceId().compareTo(cursor.position().historyId()) > 0;
    }

    private IncidentOverlay geofence(ResultSet row, int number) throws SQLException {
        String type = row.getString("transition");
        return overlay(OverlayType.GEOFENCE, type, row.getObject("id", UUID.class),
                row.getTimestamp("source_timestamp").toInstant(), null, row.getString("severity"),
                "Geofence transition — " + type, null, null, null);
    }

    private IncidentOverlay speed(ResultSet row, int number) throws SQLException {
        return overlay(OverlayType.SPEED, "SPEEDING", row.getObject("id", UUID.class),
                row.getTimestamp("start_source_timestamp").toInstant(), instant(row, "end_source_timestamp"),
                row.getString("severity"), "Speed-monitoring episode — technical evidence",
                uuid(row, "trip_id"), uuid(row, "route_id"), row.getString("route_version"));
    }

    private IncidentOverlay routeDeviation(ResultSet row, int number) throws SQLException {
        String review = row.getString("review_status");
        return overlay(OverlayType.ROUTE_DEVIATION, "ROUTE_DEVIATION", row.getObject("id", UUID.class),
                row.getTimestamp("start_source_timestamp").toInstant(), instant(row, "end_source_timestamp"),
                row.getString("severity"), "Route-deviation episode — technical evidence (" + review + ")",
                uuid(row, "trip_id"), uuid(row, "route_id"), row.getString("route_version"));
    }

    private IncidentOverlay routeDeviationReview(ResultSet row, int number) throws SQLException {
        String review = row.getString("status");
        return overlay(OverlayType.ROUTE_DEVIATION, "ROUTE_DEVIATION_REVIEW",
                row.getObject("id", UUID.class), row.getTimestamp("reviewed_at").toInstant(), null,
                row.getString("severity"), "Route-deviation review — technical evidence (" + review + ")",
                uuid(row, "trip_id"), uuid(row, "route_id"), row.getString("route_version"));
    }

    private static IncidentOverlay overlay(OverlayType type, String incidentType, UUID id, Instant start,
            Instant end, String severity, String label, UUID tripId, UUID routeId, String routeVersion) {
        return new IncidentOverlay(type, ReplayQueryPolicy.acceptance(type), incidentType, id, start, end,
                severity, label, tripId, routeId, routeVersion);
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static UUID uuid(ResultSet row, String column) throws SQLException {
        return row.getObject(column, UUID.class);
    }
}
