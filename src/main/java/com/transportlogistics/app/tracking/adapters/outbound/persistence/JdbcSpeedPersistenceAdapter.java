package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.domain.speed.ResolvedSpeedThreshold;
import com.transportlogistics.app.tracking.domain.speed.SpeedAttribution;
import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedRule;
import com.transportlogistics.app.tracking.domain.speed.SpeedingEpisode;
import com.transportlogistics.app.tracking.domain.speed.VehicleSpeedState;
import com.transportlogistics.app.tracking.ports.outbound.SpeedRuleRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodeRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleSpeedStateRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class JdbcSpeedPersistenceAdapter implements SpeedRuleRepositoryPort,
        VehicleSpeedStateRepositoryPort, SpeedingEpisodeRepositoryPort {
    private static final int MAX_PAGE_SIZE = 500;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcSpeedPersistenceAdapter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Override
    public SpeedRule save(SpeedRule rule, long expectedVersion) {
        if (expectedVersion < 0) throw new IllegalArgumentException("Expected version cannot be negative");
        return Objects.requireNonNull(transactions.execute(transaction -> {
            Optional<SpeedRule> existing = findRule(rule.tenantId(), rule.id());
            try {
                if (existing.isEmpty()) {
                    if (expectedVersion != 0 || rule.ruleVersion() != 1) throw staleRule();
                    jdbc.update("""
                            INSERT INTO tracking_speed_rule(
                             id,tenant_id,name,scope,route_id,route_version,threshold_kph,lifecycle,
                             version,effective_at,created_at,updated_at)
                            VALUES(?,?,?,?,?,?,?,?,?,?,now(),now())
                            """, rule.id(), rule.tenantId(), rule.name(), rule.scope().name(),
                            rule.routeId(), rule.routeVersion(), rule.thresholdKph().value(),
                            rule.lifecycle().name(), rule.ruleVersion(), timestamp(rule.effectiveAt()));
                } else {
                    if (rule.ruleVersion() != expectedVersion + 1) throw staleRule();
                    int changed = jdbc.update("""
                            UPDATE tracking_speed_rule SET name=?,scope=?,route_id=?,route_version=?,
                             threshold_kph=?,lifecycle=?,version=?,effective_at=?,updated_at=now()
                            WHERE tenant_id=? AND id=? AND version=?
                            """, rule.name(), rule.scope().name(), rule.routeId(), rule.routeVersion(),
                            rule.thresholdKph().value(), rule.lifecycle().name(), rule.ruleVersion(),
                            timestamp(rule.effectiveAt()), rule.tenantId(), rule.id(), expectedVersion);
                    if (changed != 1) throw staleRule();
                }
            } catch (DataIntegrityViolationException exception) {
                throw conflict("SPEED_RULE_CONFLICT", "Speed rule conflicts with Tenant configuration");
            }
            return findRule(rule.tenantId(), rule.id()).orElseThrow();
        }));
    }

    @Override
    public Optional<SpeedRule> findRule(UUID tenantId, UUID ruleId) {
        return jdbc.query("SELECT * FROM tracking_speed_rule WHERE tenant_id=? AND id=?",
                this::mapRule, tenantId, ruleId).stream().findFirst();
    }

    @Override
    public Optional<SpeedRule> findActiveRouteRule(UUID tenantId, UUID routeId, String routeVersion) {
        return jdbc.query("""
                SELECT * FROM tracking_speed_rule
                WHERE tenant_id=? AND scope='ROUTE_VERSION' AND route_id=? AND route_version=?
                  AND lifecycle='ACTIVE'
                """, this::mapRule, tenantId, routeId, routeVersion).stream().findFirst();
    }

    @Override
    public Optional<SpeedRule> findActiveTenantRule(UUID tenantId) {
        return jdbc.query("""
                SELECT * FROM tracking_speed_rule
                WHERE tenant_id=? AND scope='TENANT' AND lifecycle='ACTIVE'
                """, this::mapRule, tenantId).stream().findFirst();
    }

    @Override
    public List<SpeedRule> find(UUID tenantId, SpeedRule.Scope scope,
                                SpeedRule.Lifecycle lifecycle, int page, int size) {
        requirePage(page, size);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_speed_rule WHERE tenant_id=?
                  AND (CAST(? AS varchar) IS NULL OR scope=?)
                  AND (CAST(? AS varchar) IS NULL OR lifecycle=?)
                ORDER BY name,id LIMIT ? OFFSET ?
                """, this::mapRule, tenantId, name(scope), name(scope), name(lifecycle),
                name(lifecycle), size, page * size));
    }

    @Override
    public long count(UUID tenantId, SpeedRule.Scope scope, SpeedRule.Lifecycle lifecycle) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM tracking_speed_rule WHERE tenant_id=?
                  AND (CAST(? AS varchar) IS NULL OR scope=?)
                  AND (CAST(? AS varchar) IS NULL OR lifecycle=?)
                """, Long.class, tenantId, name(scope), name(scope), name(lifecycle), name(lifecycle));
        return count == null ? 0 : count;
    }

    @Override
    public Optional<VehicleSpeedState> findForUpdate(UUID tenantId, UUID vehicleId) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?::text,50))",
                String.class, tenantId + ":speed:" + vehicleId);
        return findState(tenantId, vehicleId, true);
    }

    @Override
    public VehicleSpeedState save(VehicleSpeedState state, long expectedVersion) {
        return Objects.requireNonNull(transactions.execute(transaction -> {
            jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?::text,50))",
                    String.class, state.tenantId() + ":speed:" + state.vehicleId());
            Optional<VehicleSpeedState> existing = findState(state.tenantId(), state.vehicleId(), true);
            try {
                if (existing.isEmpty()) {
                    if (expectedVersion != 0 || state.version() != 0) throw staleState();
                    jdbc.update("""
                            INSERT INTO tracking_speed_state(
                             tenant_id,vehicle_id,state,availability,effective_rule_id,
                             effective_rule_version,candidate_position_id,candidate_source_timestamp,
                             candidate_observed_speed_kph,candidate_sample_count,active_episode_id,
                             last_evaluated_source_timestamp,last_evaluated_position_id,version)
                            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                            """, state.tenantId(), state.vehicleId(), state.monitoringState().name(),
                            state.availability().name(), state.effectiveRuleId(),
                            nullablePositive(state.effectiveRuleId(), state.effectiveRuleVersion()),
                            state.candidateFirstPositionId(), timestamp(state.candidateSourceTimestamp()),
                            value(state.candidateObservedSpeed()), state.candidateSampleCount(),
                            state.activeEpisodeId(), timestamp(state.lastEvaluatedSourceTimestamp()),
                            state.lastEvaluatedPositionId(), state.version());
                } else {
                    if (state.version() != expectedVersion + 1) throw staleState();
                    int changed = jdbc.update("""
                            UPDATE tracking_speed_state SET state=?,availability=?,effective_rule_id=?,
                             effective_rule_version=?,candidate_position_id=?,candidate_source_timestamp=?,
                             candidate_observed_speed_kph=?,candidate_sample_count=?,active_episode_id=?,
                             last_evaluated_source_timestamp=?,last_evaluated_position_id=?,version=?,updated_at=now()
                            WHERE tenant_id=? AND vehicle_id=? AND version=?
                            """, state.monitoringState().name(), state.availability().name(),
                            state.effectiveRuleId(), nullablePositive(state.effectiveRuleId(),
                                    state.effectiveRuleVersion()), state.candidateFirstPositionId(),
                            timestamp(state.candidateSourceTimestamp()), value(state.candidateObservedSpeed()),
                            state.candidateSampleCount(), state.activeEpisodeId(),
                            timestamp(state.lastEvaluatedSourceTimestamp()), state.lastEvaluatedPositionId(),
                            state.version(), state.tenantId(), state.vehicleId(), expectedVersion);
                    if (changed != 1) throw staleState();
                }
            } catch (DataIntegrityViolationException exception) {
                throw staleState();
            }
            return findState(state.tenantId(), state.vehicleId(), false).orElseThrow();
        }));
    }

    @Override
    public List<VehicleSpeedState> find(UUID tenantId,
                                        VehicleSpeedState.MonitoringState state, int page, int size) {
        requirePage(page, size);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_speed_state WHERE tenant_id=?
                  AND (CAST(? AS varchar) IS NULL OR state=?)
                ORDER BY vehicle_id LIMIT ? OFFSET ?
                """, this::mapState, tenantId, name(state), name(state), size, page * size));
    }

    @Override
    public long count(UUID tenantId, VehicleSpeedState.MonitoringState state) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM tracking_speed_state WHERE tenant_id=?
                  AND (CAST(? AS varchar) IS NULL OR state=?)
                """, Long.class, tenantId, name(state), name(state));
        return count == null ? 0 : count;
    }

    @Override
    public SpeedingEpisode save(SpeedingEpisode episode) {
        return Objects.requireNonNull(transactions.execute(transaction -> {
            Optional<SpeedingEpisode> existing = findEpisode(episode.tenantId(), episode.id());
            if (existing.isEmpty()) {
                try {
                    jdbc.update("""
                            INSERT INTO tracking_speed_episode(
                             id,tenant_id,vehicle_id,trip_id,driver_id,route_id,route_version,rule_id,
                             rule_version,threshold_source,effective_threshold_kph,start_source_timestamp,
                             confirmation_source_timestamp,end_source_timestamp,max_observed_speed_kph,
                             eligible_above_threshold_sample_count,severity,repeat_count,
                             first_candidate_position_id,confirming_position_id)
                            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING
                            """, episode.id(), episode.tenantId(), episode.vehicleId(),
                            episode.attribution().tripId(), episode.attribution().driverId(),
                            episode.attribution().routeId(), episode.attribution().routeVersion(),
                            episode.ruleId(), episode.ruleVersion(), episode.thresholdSource().name(),
                            episode.effectiveThresholdKph().value(), timestamp(episode.startSourceTimestamp()),
                            timestamp(episode.confirmationSourceTimestamp()),
                            timestamp(episode.endSourceTimestamp()), episode.maxObservedSpeedKph().value(),
                            episode.eligibleAboveThresholdSampleCount(), episode.severity().name(),
                            episode.repeatCount(), episode.firstPositionId(), episode.confirmingPositionId());
                } catch (DataIntegrityViolationException exception) {
                    throw conflict("SPEED_EPISODE_CONFLICT", "Speed episode conflicts with existing evidence");
                }
            } else if (!existing.get().equals(episode)) {
                SpeedingEpisode stored = existing.get();
                if (!stored.open() || !sameIdentity(stored, episode)) {
                    throw conflict("SPEED_EPISODE_IMMUTABLE", "Speed episode evidence is immutable");
                }
                try {
                    int changed = jdbc.update("""
                            UPDATE tracking_speed_episode SET max_observed_speed_kph=?,
                             eligible_above_threshold_sample_count=?,end_source_timestamp=?,updated_at=now()
                            WHERE tenant_id=? AND id=? AND end_source_timestamp IS NULL
                            """, episode.maxObservedSpeedKph().value(),
                            episode.eligibleAboveThresholdSampleCount(), timestamp(episode.endSourceTimestamp()),
                            episode.tenantId(), episode.id());
                    if (changed != 1) throw conflict("SPEED_EPISODE_STALE", "Speed episode is unavailable");
                } catch (DataIntegrityViolationException exception) {
                    throw conflict("SPEED_EPISODE_CONFLICT", "Speed episode progression is invalid");
                }
            }
            return findEpisode(episode.tenantId(), episode.id()).orElseThrow();
        }));
    }

    @Override
    public Optional<SpeedingEpisode> findEpisode(UUID tenantId, UUID episodeId) {
        return jdbc.query("SELECT * FROM tracking_speed_episode WHERE tenant_id=? AND id=?",
                this::mapEpisode, tenantId, episodeId).stream().findFirst();
    }

    @Override
    public Optional<SpeedingEpisode> findActive(UUID tenantId, UUID vehicleId) {
        return jdbc.query("""
                SELECT * FROM tracking_speed_episode
                WHERE tenant_id=? AND vehicle_id=? AND end_source_timestamp IS NULL
                """, this::mapEpisode, tenantId, vehicleId).stream().findFirst();
    }

    @Override
    public Optional<SpeedingEpisode> findLatestClosed(UUID tenantId, UUID vehicleId,
                                                       UUID ruleId, long ruleVersion) {
        return jdbc.query("""
                SELECT * FROM tracking_speed_episode
                WHERE tenant_id=? AND vehicle_id=? AND rule_id=? AND rule_version=?
                  AND end_source_timestamp IS NOT NULL
                ORDER BY end_source_timestamp DESC,id DESC LIMIT 1
                """, this::mapEpisode, tenantId, vehicleId, ruleId, ruleVersion).stream().findFirst();
    }

    @Override
    public List<SpeedingEpisode> find(UUID tenantId, UUID vehicleId, UUID driverId,
                                      Instant from, Instant to, String cursor, int limit) {
        requireLimit(limit);
        Cursor parsed = Cursor.parse(cursor);
        List<Object> arguments = new ArrayList<>();
        arguments.add(tenantId);
        arguments.add(vehicleId);
        arguments.add(vehicleId);
        arguments.add(driverId);
        arguments.add(driverId);
        arguments.add(timestamp(from));
        arguments.add(timestamp(from));
        arguments.add(timestamp(to));
        arguments.add(timestamp(to));
        arguments.add(timestamp(parsed.time()));
        arguments.add(timestamp(parsed.time()));
        arguments.add(timestamp(parsed.time()));
        arguments.add(parsed.id());
        arguments.add(limit);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_speed_episode WHERE tenant_id=?
                  AND (CAST(? AS uuid) IS NULL OR vehicle_id=CAST(? AS uuid))
                  AND (CAST(? AS uuid) IS NULL OR driver_id=CAST(? AS uuid))
                  AND (CAST(? AS timestamptz) IS NULL OR start_source_timestamp>=CAST(? AS timestamptz))
                  AND (CAST(? AS timestamptz) IS NULL OR start_source_timestamp<=CAST(? AS timestamptz))
                  AND (CAST(? AS timestamptz) IS NULL OR start_source_timestamp<CAST(? AS timestamptz)
                    OR (start_source_timestamp=CAST(? AS timestamptz) AND id<CAST(? AS uuid)))
                ORDER BY start_source_timestamp DESC,id DESC LIMIT ?
                """, this::mapEpisode, arguments.toArray()));
    }

    private Optional<VehicleSpeedState> findState(UUID tenantId, UUID vehicleId, boolean lock) {
        String suffix = lock ? " FOR UPDATE" : "";
        return jdbc.query("SELECT * FROM tracking_speed_state WHERE tenant_id=? AND vehicle_id=?" + suffix,
                this::mapState, tenantId, vehicleId).stream().findFirst();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private SpeedRule mapRule(ResultSet row, int rowNumber) throws SQLException {
        return new SpeedRule(uuid(row, "id"), uuid(row, "tenant_id"), row.getString("name"),
                SpeedRule.Scope.valueOf(row.getString("scope")), nullableUuid(row, "route_id"),
                row.getString("route_version"), new SpeedKph(row.getBigDecimal("threshold_kph")),
                SpeedRule.Lifecycle.valueOf(row.getString("lifecycle")), row.getLong("version"),
                instant(row, "effective_at"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private VehicleSpeedState mapState(ResultSet row, int rowNumber) throws SQLException {
        UUID ruleId = nullableUuid(row, "effective_rule_id");
        return new VehicleSpeedState(uuid(row, "tenant_id"), uuid(row, "vehicle_id"),
                VehicleSpeedState.MonitoringState.valueOf(row.getString("state")),
                VehicleSpeedState.Availability.valueOf(row.getString("availability")), ruleId,
                ruleId == null ? 0 : row.getLong("effective_rule_version"),
                nullableUuid(row, "candidate_position_id"), instant(row, "candidate_source_timestamp"),
                speed(row, "candidate_observed_speed_kph"), row.getInt("candidate_sample_count"),
                nullableUuid(row, "active_episode_id"), instant(row, "last_evaluated_source_timestamp"),
                nullableUuid(row, "last_evaluated_position_id"), row.getLong("version"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private SpeedingEpisode mapEpisode(ResultSet row, int rowNumber) throws SQLException {
        return new SpeedingEpisode(uuid(row, "id"), uuid(row, "tenant_id"), uuid(row, "vehicle_id"),
                new SpeedAttribution(nullableUuid(row, "trip_id"), nullableUuid(row, "driver_id"),
                        nullableUuid(row, "route_id"), row.getString("route_version")),
                uuid(row, "rule_id"), row.getLong("rule_version"),
                ResolvedSpeedThreshold.ThresholdSource.valueOf(row.getString("threshold_source")),
                new SpeedKph(row.getBigDecimal("effective_threshold_kph")),
                instant(row, "start_source_timestamp"), instant(row, "confirmation_source_timestamp"),
                instant(row, "end_source_timestamp"), uuid(row, "first_candidate_position_id"),
                uuid(row, "confirming_position_id"), new SpeedKph(row.getBigDecimal("max_observed_speed_kph")),
                row.getInt("eligible_above_threshold_sample_count"),
                SpeedingEpisode.Severity.valueOf(row.getString("severity")), row.getInt("repeat_count"));
    }

    private static boolean sameIdentity(SpeedingEpisode left, SpeedingEpisode right) {
        return left.id().equals(right.id()) && left.tenantId().equals(right.tenantId())
                && left.vehicleId().equals(right.vehicleId()) && left.attribution().equals(right.attribution())
                && left.ruleId().equals(right.ruleId()) && left.ruleVersion() == right.ruleVersion()
                && left.thresholdSource() == right.thresholdSource()
                && left.effectiveThresholdKph().equals(right.effectiveThresholdKph())
                && left.startSourceTimestamp().equals(right.startSourceTimestamp())
                && left.confirmationSourceTimestamp().equals(right.confirmationSourceTimestamp())
                && left.severity() == right.severity() && left.repeatCount() == right.repeatCount()
                && left.firstPositionId().equals(right.firstPositionId())
                && left.confirmingPositionId().equals(right.confirmingPositionId());
    }

    private static void requirePage(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Page cannot be negative");
        requireLimit(size);
    }

    private static void requireLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Limit must be between 1 and 500");
        }
    }

    private static String name(Enum<?> value) { return value == null ? null : value.name(); }
    private static Object value(SpeedKph speed) { return speed == null ? null : speed.value(); }
    private static Long nullablePositive(UUID id, long value) { return id == null ? null : value; }
    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static UUID uuid(ResultSet row, String column) throws SQLException {
        return row.getObject(column, UUID.class);
    }
    private static UUID nullableUuid(ResultSet row, String column) throws SQLException {
        return row.getObject(column, UUID.class);
    }
    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
    private static SpeedKph speed(ResultSet row, String column) throws SQLException {
        var value = row.getBigDecimal(column);
        return value == null ? null : new SpeedKph(value);
    }
    private static BusinessRuleException staleRule() {
        return conflict("SPEED_RULE_STALE_VERSION", "Speed rule persistence version is stale");
    }
    private static BusinessRuleException staleState() {
        return conflict("SPEED_STATE_STALE_VERSION", "Speed state persistence version is stale");
    }
    private static BusinessRuleException conflict(String code, String message) {
        return new BusinessRuleException(code, message);
    }

    private record Cursor(Instant time, UUID id) {
        private static Cursor parse(String value) {
            if (value == null || value.isBlank()) return new Cursor(null, null);
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
                String[] parts = decoded.split("\\|", -1);
                return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid speed episode cursor", exception);
            }
        }
    }
}
