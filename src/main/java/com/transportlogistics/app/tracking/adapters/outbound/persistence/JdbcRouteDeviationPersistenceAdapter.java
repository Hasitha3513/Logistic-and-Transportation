package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationEpisode;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationReview;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationRule;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationReviewRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationRuleRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
final class JdbcRouteDeviationPersistenceAdapter implements RouteDeviationRuleRepositoryPort,
        RouteDeviationEpisodeRepositoryPort, RouteDeviationReviewRepositoryPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcRouteDeviationPersistenceAdapter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public Optional<RouteDeviationRule> findActive(
            UUID tenantId, UUID routeId, RouteVersion routeVersion) {
        return jdbc.query("""
                SELECT * FROM tracking_route_deviation_rule
                WHERE tenant_id=? AND route_id=? AND route_version=? AND lifecycle='ACTIVE'
                """, this::rule, tenantId, routeId, routeVersion.value()).stream().findFirst();
    }

    @Override
    public Optional<RouteDeviationRule> findRule(UUID tenantId, UUID ruleId) {
        requireTenant(tenantId);
        return jdbc.query("SELECT * FROM tracking_route_deviation_rule WHERE tenant_id=? AND id=?",
                this::rule, tenantId, ruleId).stream().findFirst();
    }

    @Override
    public List<RouteDeviationRule> list(UUID tenantId, RouteDeviationRule.Lifecycle lifecycle,
            int offset, int size) {
        requireTenant(tenantId);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_route_deviation_rule
                WHERE tenant_id=? AND (CAST(? AS varchar) IS NULL OR lifecycle=CAST(? AS varchar))
                ORDER BY route_id,route_version,id LIMIT ? OFFSET ?
                """, this::rule, tenantId, name(lifecycle), name(lifecycle), size, offset));
    }

    @Override
    public long count(UUID tenantId, RouteDeviationRule.Lifecycle lifecycle) {
        requireTenant(tenantId);
        Long value = jdbc.queryForObject("""
                SELECT count(*) FROM tracking_route_deviation_rule
                WHERE tenant_id=? AND (CAST(? AS varchar) IS NULL OR lifecycle=CAST(? AS varchar))
                """, Long.class, tenantId, name(lifecycle), name(lifecycle));
        return value == null ? 0 : value;
    }

    @Override
    public RouteDeviationRule save(RouteDeviationRule rule) {
        requireTenant(rule.tenantId());
        transactions.executeWithoutResult(status -> {
            lock(rule.tenantId(), rule.id());
            int updated = jdbc.update("""
                    UPDATE tracking_route_deviation_rule SET
                      configured_tolerance_meters=?,lifecycle=?,rule_version=?,manage_version=?,
                      effective_at=?,updated_at=now()
                    WHERE tenant_id=? AND id=? AND manage_version<?
                    """, rule.configuredTolerance().value(), rule.lifecycle().name(),
                    rule.ruleVersion(), rule.manageVersion(), timestamp(rule.effectiveAt()),
                    rule.tenantId(), rule.id(), rule.manageVersion());
            if (updated == 0 && !exists("tracking_route_deviation_rule", rule.tenantId(), rule.id())) {
                jdbc.update("""
                        INSERT INTO tracking_route_deviation_rule(
                          id,tenant_id,route_id,route_version,configured_tolerance_meters,lifecycle,
                          rule_version,manage_version,effective_at)
                        VALUES(?,?,?,?,?,?,?,?,?)
                        """, rule.id(), rule.tenantId(), rule.routeId(), rule.routeVersion().value(),
                        rule.configuredTolerance().value(), rule.lifecycle().name(), rule.ruleVersion(),
                        rule.manageVersion(), timestamp(rule.effectiveAt()));
            } else if (updated == 0) {
                throw new IllegalStateException("Route-deviation rule version is stale");
            }
        });
        return rule;
    }

    @Override
    public RouteDeviationEpisode save(RouteDeviationEpisode episode) {
        requireTenant(episode.tenantId());
        transactions.executeWithoutResult(status -> {
            lock(episode.tenantId(), episode.id());
            int inserted = jdbc.update("""
                    INSERT INTO tracking_route_deviation_episode(
                      id,tenant_id,vehicle_id,trip_id,driver_id,route_id,route_version,rule_id,rule_version,
                      configured_tolerance_meters,effective_tolerance_meters,first_candidate_position_id,
                      confirming_position_id,start_source_timestamp,confirmation_source_timestamp,
                      end_source_timestamp,maximum_distance_meters,eligible_outside_sample_count,severity,
                      review_status,review_version,terminal_outcome,disruption_id,distance_high_escalated,
                      review_rejected_escalated)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                    """, episode.id(), episode.tenantId(), episode.vehicleId(), episode.tripId(), episode.driverId(),
                    episode.routeId(), episode.routeVersion().value(), episode.ruleId(), episode.ruleVersion(),
                    distance(episode.configuredTolerance()), distance(episode.effectiveTolerance()),
                    episode.firstCandidatePositionId(), episode.confirmingPositionId(),
                    timestamp(episode.startSourceTimestamp()), timestamp(episode.confirmationSourceTimestamp()),
                    timestamp(episode.endSourceTimestamp()), distance(episode.maximumDistance()),
                    episode.eligibleOutsideSampleCount(), episode.severity().name(), episode.reviewStatus().name(),
                    episode.reviewVersion(), name(episode.terminalOutcome()), episode.disruptionId(),
                    episode.distanceHighEscalated(), episode.reviewRejectedEscalated());
            if (inserted == 0) updateEpisode(episode);
        });
        return episode;
    }

    private void updateEpisode(RouteDeviationEpisode episode) {
        int updated = jdbc.update("""
                UPDATE tracking_route_deviation_episode SET end_source_timestamp=?,maximum_distance_meters=?,
                  eligible_outside_sample_count=?,severity=?,review_status=?,review_version=?,terminal_outcome=?,
                  disruption_id=?,distance_high_escalated=?,review_rejected_escalated=?,lock_version=lock_version+1,
                  updated_at=now()
                WHERE tenant_id=? AND id=? AND review_version<=? AND eligible_outside_sample_count<=?
                """, timestamp(episode.endSourceTimestamp()), distance(episode.maximumDistance()),
                episode.eligibleOutsideSampleCount(), episode.severity().name(), episode.reviewStatus().name(),
                episode.reviewVersion(), name(episode.terminalOutcome()), episode.disruptionId(),
                episode.distanceHighEscalated(), episode.reviewRejectedEscalated(), episode.tenantId(), episode.id(),
                episode.reviewVersion(), episode.eligibleOutsideSampleCount());
        if (updated == 0) throw new IllegalStateException("Route-deviation episode version is stale");
    }

    @Override
    public Optional<RouteDeviationEpisode> find(UUID tenantId, UUID episodeId) {
        requireTenant(tenantId);
        return jdbc.query("SELECT * FROM tracking_route_deviation_episode WHERE tenant_id=? AND id=?",
                this::episode, tenantId, episodeId).stream().findFirst();
    }

    @Override
    public Optional<RouteDeviationEpisode> lockAndFind(UUID tenantId, UUID episodeId) {
        requireTenant(tenantId);
        lock(tenantId, episodeId);
        return find(tenantId, episodeId);
    }

    @Override
    public Optional<RouteDeviationEpisode> findOpen(UUID tenantId, UUID vehicleId) {
        requireTenant(tenantId);
        return jdbc.query("""
                SELECT * FROM tracking_route_deviation_episode
                WHERE tenant_id=? AND vehicle_id=? AND end_source_timestamp IS NULL
                """, this::episode, tenantId, vehicleId).stream().findFirst();
    }

    @Override
    public List<RouteDeviationEpisode> history(UUID tenantId, UUID vehicleId, Instant from,
            Instant to, String cursor, int limit) {
        requireTenant(tenantId);
        if (from == null || to == null || !from.isBefore(to) || limit < 1 || limit > 500) {
            throw new IllegalArgumentException("A bounded history query is required");
        }
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_route_deviation_episode
                WHERE tenant_id=? AND vehicle_id=? AND confirmation_source_timestamp>=?
                  AND confirmation_source_timestamp<? ORDER BY confirmation_source_timestamp DESC,id DESC LIMIT ?
                """, this::episode, tenantId, vehicleId, timestamp(from), timestamp(to), limit));
    }

    @Override
    public List<RouteDeviationEpisode> search(UUID tenantId, UUID vehicleId, UUID tripId,
            UUID routeId, RouteDeviationEpisode.Severity severity, Boolean open, Instant from,
            Instant to, Instant cursorTime, UUID cursorId, int limit) {
        requireTenant(tenantId);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_route_deviation_episode
                WHERE tenant_id=?
                  AND (CAST(? AS uuid) IS NULL OR vehicle_id=CAST(? AS uuid))
                  AND (CAST(? AS uuid) IS NULL OR trip_id=CAST(? AS uuid))
                  AND (CAST(? AS uuid) IS NULL OR route_id=CAST(? AS uuid))
                  AND (CAST(? AS varchar) IS NULL OR severity=CAST(? AS varchar))
                  AND (CAST(? AS boolean) IS NULL
                       OR (CAST(? AS boolean)=TRUE AND end_source_timestamp IS NULL)
                       OR (CAST(? AS boolean)=FALSE AND end_source_timestamp IS NOT NULL))
                  AND start_source_timestamp>=? AND start_source_timestamp<=?
                  AND (CAST(? AS timestamptz) IS NULL OR (start_source_timestamp,id)<
                       (CAST(? AS timestamptz),CAST(? AS uuid)))
                ORDER BY start_source_timestamp DESC,id DESC LIMIT ?
                """, this::episode, tenantId, vehicleId, vehicleId, tripId, tripId, routeId, routeId,
                name(severity), name(severity), open, open, open, timestamp(from), timestamp(to),
                timestamp(cursorTime), timestamp(cursorTime), cursorId, limit));
    }

    @Override
    public RouteDeviationReview append(RouteDeviationReview review) {
        requireTenant(review.tenantId());
        try {
            jdbc.update("""
                    INSERT INTO tracking_route_deviation_review(
                      id,tenant_id,episode_id,status,reason,note,reviewer_id,reviewed_at,
                      review_version,compensates_review_id) VALUES(?,?,?,?,?,?,?,?,?,?)
                    """, review.id(), review.tenantId(), review.episodeId(), review.status().name(),
                    review.reason().name(), review.note(), review.reviewerId(), timestamp(review.reviewedAt()),
                    review.reviewVersion(), review.compensatesReviewId());
            return review;
        } catch (DuplicateKeyException exception) {
            return history(review.tenantId(), review.episodeId()).stream()
                    .filter(existing -> existing.reviewVersion() == review.reviewVersion())
                    .findFirst().filter(review::equals)
                    .orElseThrow(() -> new IllegalStateException("Review version already exists", exception));
        }
    }

    @Override
    public List<RouteDeviationReview> history(UUID tenantId, UUID episodeId) {
        requireTenant(tenantId);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_route_deviation_review
                WHERE tenant_id=? AND episode_id=? ORDER BY review_version
                """, this::review, tenantId, episodeId));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private RouteDeviationRule rule(ResultSet row, int number) throws SQLException {
        return new RouteDeviationRule(uuid(row, "id"), uuid(row, "tenant_id"), uuid(row, "route_id"),
                new RouteVersion(row.getString("route_version")), new DistanceMeters(row.getBigDecimal("configured_tolerance_meters")),
                RouteDeviationRule.Lifecycle.valueOf(row.getString("lifecycle")), row.getLong("rule_version"),
                row.getLong("manage_version"), instant(row, "effective_at"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private RouteDeviationEpisode episode(ResultSet row, int number) throws SQLException {
        return new RouteDeviationEpisode(uuid(row, "id"), uuid(row, "tenant_id"), uuid(row, "vehicle_id"),
                uuid(row, "trip_id"), uuid(row, "driver_id"), uuid(row, "route_id"),
                routeVersion(row, "route_version"), uuid(row, "rule_id"), row.getLong("rule_version"),
                meters(row, "configured_tolerance_meters"), meters(row, "effective_tolerance_meters"),
                uuid(row, "first_candidate_position_id"), uuid(row, "confirming_position_id"),
                instant(row, "start_source_timestamp"), instant(row, "confirmation_source_timestamp"),
                instant(row, "end_source_timestamp"), meters(row, "maximum_distance_meters"),
                row.getInt("eligible_outside_sample_count"),
                RouteDeviationEpisode.Severity.valueOf(row.getString("severity")),
                RouteDeviationReview.Status.valueOf(row.getString("review_status")), row.getLong("review_version"),
                enumValue(RouteDeviationEpisode.TerminalOutcome.class, row.getString("terminal_outcome")),
                uuid(row, "disruption_id"), row.getBoolean("distance_high_escalated"),
                row.getBoolean("review_rejected_escalated"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private RouteDeviationReview review(ResultSet row, int number) throws SQLException {
        return new RouteDeviationReview(uuid(row, "id"), uuid(row, "tenant_id"), uuid(row, "episode_id"),
                RouteDeviationReview.Status.valueOf(row.getString("status")),
                RouteDeviationReview.Reason.valueOf(row.getString("reason")), row.getString("note"),
                uuid(row, "reviewer_id"), instant(row, "reviewed_at"), row.getLong("review_version"),
                uuid(row, "compensates_review_id"));
    }

    private void lock(UUID tenantId, UUID identity) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(? || ':' || ?, 0))",
                row -> { }, tenantId.toString(), identity.toString());
    }

    private boolean exists(String table, UUID tenantId, UUID id) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table
                + " WHERE tenant_id=? AND id=?", Integer.class, tenantId, id);
        return count != null && count > 0;
    }

    private static void requireTenant(UUID tenantId) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID is required");
    }

    private static java.math.BigDecimal distance(DistanceMeters value) { return value == null ? null : value.value(); }
    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static String name(Enum<?> value) { return value == null ? null : value.name(); }
    private static UUID uuid(ResultSet row, String column) throws SQLException {
        Object value = row.getObject(column); return value == null ? null : (UUID) value;
    }
    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column); return value == null ? null : value.toInstant();
    }
    private static DistanceMeters meters(ResultSet row, String column) throws SQLException {
        var value = row.getBigDecimal(column); return value == null ? null : new DistanceMeters(value);
    }
    private static RouteVersion routeVersion(ResultSet row, String column) throws SQLException {
        String value = row.getString(column); return value == null ? null : new RouteVersion(value);
    }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
