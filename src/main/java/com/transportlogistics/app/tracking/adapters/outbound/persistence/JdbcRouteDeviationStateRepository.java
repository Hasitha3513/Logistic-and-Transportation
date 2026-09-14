package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.routedeviation.DistanceMeters;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteDeviationAvailability;
import com.transportlogistics.app.tracking.domain.routedeviation.RoutePoint;
import com.transportlogistics.app.tracking.domain.routedeviation.RouteVersion;
import com.transportlogistics.app.tracking.domain.routedeviation.VehicleRouteDeviationState;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationStateRepositoryPort;
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
final class JdbcRouteDeviationStateRepository implements RouteDeviationStateRepositoryPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcRouteDeviationStateRepository(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public Optional<VehicleRouteDeviationState> find(UUID tenantId, UUID vehicleId) {
        requireTenant(tenantId);
        return jdbc.query("SELECT * FROM tracking_route_deviation_state WHERE tenant_id=? AND vehicle_id=?",
                this::map, tenantId, vehicleId).stream().findFirst();
    }

    @Override
    public List<VehicleRouteDeviationState> list(UUID tenantId,
            VehicleRouteDeviationState.State state, int offset, int size) {
        requireTenant(tenantId);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_route_deviation_state
                WHERE tenant_id=? AND (CAST(? AS varchar) IS NULL OR stable_state=CAST(? AS varchar))
                ORDER BY vehicle_id LIMIT ? OFFSET ?
                """, this::map, tenantId, state == null ? null : state.name(),
                state == null ? null : state.name(), size, offset));
    }

    @Override
    public long count(UUID tenantId, VehicleRouteDeviationState.State state) {
        requireTenant(tenantId);
        Long value = jdbc.queryForObject("""
                SELECT count(*) FROM tracking_route_deviation_state
                WHERE tenant_id=? AND (CAST(? AS varchar) IS NULL OR stable_state=CAST(? AS varchar))
                """, Long.class, tenantId, state == null ? null : state.name(),
                state == null ? null : state.name());
        return value == null ? 0 : value;
    }

    @Override
    public Optional<VehicleRouteDeviationState> lockAndFind(UUID tenantId, UUID vehicleId) {
        requireTenant(tenantId);
        lock(tenantId, vehicleId);
        return find(tenantId, vehicleId);
    }

    @Override
    public VehicleRouteDeviationState save(VehicleRouteDeviationState state) {
        requireTenant(state.tenantId());
        transactions.executeWithoutResult(status -> {
            lock(state.tenantId(), state.vehicleId());
            VehicleRouteDeviationState current = find(state.tenantId(), state.vehicleId()).orElse(null);
            if (current != null && compareSource(state, current) < 0) {
                throw new IllegalStateException("Route-deviation state is stale");
            }
            VehicleRouteDeviationState.Candidate candidate = state.candidate();
            jdbc.update("""
                    INSERT INTO tracking_route_deviation_state(
                      tenant_id,vehicle_id,stable_state,availability,current_trip_id,route_id,route_version,
                      rule_id,rule_version,configured_tolerance_meters,effective_tolerance_meters,
                      candidate_position_id,candidate_source_timestamp,candidate_trip_id,candidate_driver_id,
                      candidate_route_id,candidate_route_version,candidate_rule_id,candidate_rule_version,
                      candidate_configured_tolerance_meters,candidate_effective_tolerance_meters,
                      candidate_distance_meters,candidate_longitude,candidate_latitude,candidate_accuracy_meters,
                      active_episode_id,last_source_timestamp,last_position_id)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT (tenant_id,vehicle_id) DO UPDATE SET
                      stable_state=excluded.stable_state,availability=excluded.availability,
                      current_trip_id=excluded.current_trip_id,route_id=excluded.route_id,
                      route_version=excluded.route_version,rule_id=excluded.rule_id,
                      rule_version=excluded.rule_version,configured_tolerance_meters=excluded.configured_tolerance_meters,
                      effective_tolerance_meters=excluded.effective_tolerance_meters,
                      candidate_position_id=excluded.candidate_position_id,
                      candidate_source_timestamp=excluded.candidate_source_timestamp,
                      candidate_trip_id=excluded.candidate_trip_id,candidate_driver_id=excluded.candidate_driver_id,
                      candidate_route_id=excluded.candidate_route_id,candidate_route_version=excluded.candidate_route_version,
                      candidate_rule_id=excluded.candidate_rule_id,candidate_rule_version=excluded.candidate_rule_version,
                      candidate_configured_tolerance_meters=excluded.candidate_configured_tolerance_meters,
                      candidate_effective_tolerance_meters=excluded.candidate_effective_tolerance_meters,
                      candidate_distance_meters=excluded.candidate_distance_meters,
                      candidate_longitude=excluded.candidate_longitude,candidate_latitude=excluded.candidate_latitude,
                      candidate_accuracy_meters=excluded.candidate_accuracy_meters,
                      active_episode_id=excluded.active_episode_id,last_source_timestamp=excluded.last_source_timestamp,
                      last_position_id=excluded.last_position_id,lock_version=tracking_route_deviation_state.lock_version+1,
                      updated_at=now()
                    """, state.tenantId(), state.vehicleId(), state.state().name(), state.availability().name(),
                    state.currentTripId(), state.routeId(), value(state.routeVersion()), state.ruleId(),
                    state.ruleVersion(), distance(state.configuredTolerance()), distance(state.effectiveTolerance()),
                    candidate == null ? null : candidate.positionId(), candidate == null ? null : timestamp(candidate.sourceTimestamp()),
                    candidate == null ? null : candidate.tripId(), candidate == null ? null : candidate.driverId(),
                    candidate == null ? null : candidate.routeId(), candidate == null ? null : value(candidate.routeVersion()),
                    candidate == null ? null : candidate.ruleId(), candidate == null ? null : candidate.ruleVersion(),
                    candidate == null ? null : distance(candidate.configuredTolerance()),
                    candidate == null ? null : distance(candidate.effectiveTolerance()),
                    candidate == null ? null : distance(candidate.distance()),
                    candidate == null ? null : candidate.point().longitude(),
                    candidate == null ? null : candidate.point().latitude(),
                    candidate == null ? null : distance(candidate.accuracy()), state.activeEpisodeId(),
                    timestamp(state.lastSourceTimestamp()), state.lastPositionId());
        });
        return state;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private VehicleRouteDeviationState map(ResultSet row, int number) throws SQLException {
        UUID candidateId = uuid(row, "candidate_position_id");
        VehicleRouteDeviationState.Candidate candidate = candidateId == null ? null
                : new VehicleRouteDeviationState.Candidate(candidateId, instant(row, "candidate_source_timestamp"),
                        uuid(row, "candidate_trip_id"), uuid(row, "candidate_driver_id"),
                        uuid(row, "candidate_route_id"), routeVersion(row, "candidate_route_version"),
                        uuid(row, "candidate_rule_id"), row.getLong("candidate_rule_version"),
                        meters(row, "candidate_configured_tolerance_meters"),
                        meters(row, "candidate_effective_tolerance_meters"), meters(row, "candidate_distance_meters"),
                        new RoutePoint(row.getBigDecimal("candidate_longitude"), row.getBigDecimal("candidate_latitude")),
                        meters(row, "candidate_accuracy_meters"));
        return new VehicleRouteDeviationState(uuid(row, "tenant_id"), uuid(row, "vehicle_id"),
                VehicleRouteDeviationState.State.valueOf(row.getString("stable_state")),
                RouteDeviationAvailability.valueOf(row.getString("availability")), uuid(row, "current_trip_id"),
                uuid(row, "route_id"), routeVersion(row, "route_version"), uuid(row, "rule_id"),
                row.getLong("rule_version"), meters(row, "configured_tolerance_meters"),
                meters(row, "effective_tolerance_meters"), candidate, uuid(row, "active_episode_id"),
                instant(row, "last_source_timestamp"), uuid(row, "last_position_id"));
    }

    private void lock(UUID tenantId, UUID identity) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(? || ':' || ?, 0))",
                row -> { }, tenantId.toString(), identity.toString());
    }

    private static int compareSource(VehicleRouteDeviationState left, VehicleRouteDeviationState right) {
        if (left.lastSourceTimestamp() == null) return right.lastSourceTimestamp() == null ? 0 : -1;
        if (right.lastSourceTimestamp() == null) return 1;
        int time = left.lastSourceTimestamp().compareTo(right.lastSourceTimestamp());
        return time != 0 || left.lastPositionId() == null || right.lastPositionId() == null
                ? time : left.lastPositionId().compareTo(right.lastPositionId());
    }

    private static void requireTenant(UUID tenantId) {
        if (tenantId == null) throw new IllegalArgumentException("Tenant ID is required");
    }
    private static String value(RouteVersion version) { return version == null ? null : version.value(); }
    private static java.math.BigDecimal distance(DistanceMeters value) { return value == null ? null : value.value(); }
    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
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
}
