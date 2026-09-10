package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.tracking.domain.geofence.Geofence;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceAlertPolicy;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceLifecycle;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceMembership;
import com.transportlogistics.app.tracking.domain.geofence.GeofencePolygon;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceSeverity;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransition;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransitionType;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.domain.geofence.VehicleGeofenceState;
import com.transportlogistics.app.tracking.domain.geofence.Wgs84Coordinate;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceManagementSupportPort;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionRepositoryPort;
import com.transportlogistics.app.tracking.ports.outbound.VehicleGeofenceStateRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class JdbcGeofencePersistenceAdapter implements GeofenceRepositoryPort,
        VehicleGeofenceStateRepositoryPort, GeofenceTransitionRepositoryPort,
        GeofenceManagementSupportPort {
    private static final int MAX_PAGE = 500;
    private static final int MAX_TRANSITION_PAGE = 101;
    private static final int MAX_POLYGON_BYTES = 16_384;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;

    JdbcGeofencePersistenceAdapter(
            JdbcTemplate jdbc, TransactionTemplate transactions, ObjectMapper json) {
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.json = json;
    }

    @Override
    public Geofence save(Geofence geofence, long expectedVersion) {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("Expected version cannot be negative");
        }
        String polygon = serialize(geofence.polygon());
        var bounds = geofence.polygon().boundingBox();
        return Objects.requireNonNull(transactions.execute(status -> {
            if (find(geofence.tenantId(), geofence.id()).isEmpty()) {
                if (expectedVersion != 0 || geofence.version() != 0) {
                    throw stale();
                }
                try {
                    jdbc.update("""
                            INSERT INTO tracking_geofence(
                             id,tenant_id,name,type,polygon_vertices,min_longitude,max_longitude,
                             min_latitude,max_latitude,location_id,alert_enter_enabled,alert_exit_enabled,
                             lifecycle,version,created_at,created_by,updated_at,updated_by)
                            VALUES(?,?,?,?,?::jsonb,?,?,?,?,?,?,?,?,?,?,?,?,?)
                            """, geofence.id(), geofence.tenantId(), geofence.name(),
                            geofence.type().name(), polygon, bounds.minLongitude(), bounds.maxLongitude(),
                            bounds.minLatitude(), bounds.maxLatitude(), geofence.locationId(),
                            geofence.alertPolicy().alertOnEntry(), geofence.alertPolicy().alertOnExit(),
                            geofence.lifecycle().name(), geofence.version(),
                            timestamp(geofence.createdAt()), geofence.createdBy(),
                            timestamp(geofence.updatedAt()), geofence.updatedBy());
                } catch (DataIntegrityViolationException exception) {
                    throw conflict();
                }
            } else {
                if (geofence.version() != expectedVersion + 1) {
                    throw stale();
                }
                int changed;
                try {
                    changed = jdbc.update("""
                            UPDATE tracking_geofence SET
                             name=?,type=?,polygon_vertices=?::jsonb,min_longitude=?,max_longitude=?,
                             min_latitude=?,max_latitude=?,location_id=?,alert_enter_enabled=?,
                             alert_exit_enabled=?,lifecycle=?,version=?,updated_at=?,updated_by=?
                            WHERE tenant_id=? AND id=? AND version=?
                            """, geofence.name(), geofence.type().name(), polygon,
                            bounds.minLongitude(), bounds.maxLongitude(), bounds.minLatitude(),
                            bounds.maxLatitude(), geofence.locationId(),
                            geofence.alertPolicy().alertOnEntry(), geofence.alertPolicy().alertOnExit(),
                            geofence.lifecycle().name(), geofence.version(),
                            timestamp(geofence.updatedAt()), geofence.updatedBy(), geofence.tenantId(),
                            geofence.id(), expectedVersion);
                } catch (DataIntegrityViolationException exception) {
                    throw conflict();
                }
                if (changed != 1) {
                    throw stale();
                }
            }
            return find(geofence.tenantId(), geofence.id()).orElseThrow();
        }));
    }

    @Override
    public Optional<Geofence> find(UUID tenantId, UUID geofenceId) {
        return jdbc.query("SELECT * FROM tracking_geofence WHERE tenant_id=? AND id=?",
                this::mapGeofence, tenantId, geofenceId).stream().findFirst();
    }

    @Override
    public Optional<Geofence> findForUpdate(UUID tenantId, UUID geofenceId) {
        return jdbc.query("SELECT * FROM tracking_geofence WHERE tenant_id=? AND id=? FOR UPDATE",
                this::mapGeofence, tenantId, geofenceId).stream().findFirst();
    }

    @Override
    public List<Geofence> find(UUID tenantId, GeofenceType type, GeofenceLifecycle lifecycle,
                               UUID locationId, int page, int size) {
        requirePage(page, size, MAX_PAGE);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_geofence
                WHERE tenant_id=? AND (CAST(? AS varchar) IS NULL OR type=?)
                  AND (CAST(? AS varchar) IS NULL OR lifecycle=?)
                  AND (CAST(? AS uuid) IS NULL OR location_id=CAST(? AS uuid))
                ORDER BY name,id LIMIT ? OFFSET ?
                """, this::mapGeofence, tenantId, name(type), name(type), name(lifecycle),
                name(lifecycle), locationId, locationId, size, page * size));
    }

    @Override
    public long count(UUID tenantId, GeofenceType type, GeofenceLifecycle lifecycle,
                      UUID locationId) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM tracking_geofence
                WHERE tenant_id=? AND (CAST(? AS varchar) IS NULL OR type=?)
                  AND (CAST(? AS varchar) IS NULL OR lifecycle=?)
                  AND (CAST(? AS uuid) IS NULL OR location_id=CAST(? AS uuid))
                """, Long.class, tenantId, name(type), name(type), name(lifecycle), name(lifecycle),
                locationId, locationId);
        return count == null ? 0 : count;
    }

    @Override
    public List<Geofence> findActiveCandidates(
            UUID tenantId, UUID vehicleId, double longitude, double latitude, int limit) {
        requireLimit(limit, MAX_PAGE);
        return List.copyOf(jdbc.query("""
                SELECT geofence.* FROM tracking_geofence geofence
                WHERE geofence.tenant_id=? AND geofence.lifecycle='ACTIVE' AND (
                  (geofence.min_longitude<=? AND geofence.max_longitude>=?
                   AND geofence.min_latitude<=? AND geofence.max_latitude>=?)
                  OR EXISTS(SELECT 1 FROM tracking_vehicle_geofence_state state
                    WHERE state.tenant_id=geofence.tenant_id
                      AND state.geofence_id=geofence.id AND state.vehicle_id=?))
                ORDER BY geofence.id LIMIT ?
                """, this::mapGeofence, tenantId, longitude, longitude, latitude, latitude,
                vehicleId, limit));
    }

    @Override
    public List<ActiveGeofenceReference> findActiveOutsideWithoutState(
            UUID tenantId, UUID vehicleId, double longitude, double latitude, int limit) {
        requireLimit(limit, MAX_PAGE);
        return List.copyOf(jdbc.query("""
                SELECT geofence.id,geofence.version FROM tracking_geofence geofence
                WHERE geofence.tenant_id=? AND geofence.lifecycle='ACTIVE'
                  AND NOT (geofence.min_longitude<=? AND geofence.max_longitude>=?
                    AND geofence.min_latitude<=? AND geofence.max_latitude>=?)
                  AND NOT EXISTS(SELECT 1 FROM tracking_vehicle_geofence_state state
                    WHERE state.tenant_id=geofence.tenant_id
                      AND state.geofence_id=geofence.id AND state.vehicle_id=?)
                ORDER BY geofence.id LIMIT ?
                """, (row, number) -> new ActiveGeofenceReference(
                        uuid(row, "id"), row.getLong("version")), tenantId, longitude,
                longitude, latitude, latitude, vehicleId, limit));
    }

    @Override
    public long countActiveForUpdate(UUID tenantId) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?::text,49))",
                String.class, tenantId);
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM tracking_geofence WHERE tenant_id=? AND lifecycle='ACTIVE'",
                Long.class, tenantId);
        return count == null ? 0 : count;
    }

    @Override
    public Optional<VehicleGeofenceState> findForUpdate(
            UUID tenantId, UUID geofenceId, UUID vehicleId) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?::text,50))",
                String.class, tenantId + ":" + geofenceId + ":" + vehicleId);
        return jdbc.query("""
                SELECT * FROM tracking_vehicle_geofence_state
                WHERE tenant_id=? AND geofence_id=? AND vehicle_id=? FOR UPDATE
                """, this::mapState, tenantId, geofenceId, vehicleId).stream().findFirst();
    }

    @Override
    public VehicleGeofenceState save(VehicleGeofenceState state, long expectedVersion) {
        return Objects.requireNonNull(transactions.execute(transaction -> {
            boolean exists = Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM tracking_vehicle_geofence_state
                     WHERE tenant_id=? AND geofence_id=? AND vehicle_id=?)
                    """, Boolean.class, state.tenantId(), state.geofenceId(), state.vehicleId()));
            if (!exists) {
                if (expectedVersion != 0) {
                    throw stale();
                }
                try {
                    jdbc.update("""
                            INSERT INTO tracking_vehicle_geofence_state(
                             tenant_id,geofence_id,vehicle_id,definition_version,stable_state,
                             pending_candidate,pending_count,pending_position_id,
                             last_evaluated_position_id,last_evaluated_source_timestamp,version,
                             created_at,updated_at)
                            VALUES(?,?,?,?,?,?,?,?,?,?,?,now(),now())
                            """, state.tenantId(), state.geofenceId(), state.vehicleId(),
                            state.definitionVersion(), name(state.stableState()),
                            name(state.pendingCandidate()), state.pendingCount(),
                            state.pendingPositionId(), state.lastEvaluatedPositionId(),
                            timestamp(state.lastEvaluatedSourceTimestamp()), state.version());
                } catch (DataIntegrityViolationException exception) {
                    throw stale();
                }
            } else {
                if (state.version() != expectedVersion + 1) {
                    throw stale();
                }
                int changed = jdbc.update("""
                        UPDATE tracking_vehicle_geofence_state SET definition_version=?,stable_state=?,
                         pending_candidate=?,pending_count=?,pending_position_id=?,
                         last_evaluated_position_id=?,last_evaluated_source_timestamp=?,
                         version=?,updated_at=now()
                        WHERE tenant_id=? AND geofence_id=? AND vehicle_id=? AND version=?
                        """, state.definitionVersion(), name(state.stableState()),
                        name(state.pendingCandidate()), state.pendingCount(), state.pendingPositionId(),
                        state.lastEvaluatedPositionId(), timestamp(state.lastEvaluatedSourceTimestamp()),
                        state.version(), state.tenantId(), state.geofenceId(), state.vehicleId(),
                        expectedVersion);
                if (changed != 1) {
                    throw stale();
                }
            }
            return findState(state.tenantId(), state.geofenceId(), state.vehicleId()).orElseThrow();
        }));
    }

    @Override
    public List<VehicleGeofenceState> find(
            UUID tenantId, UUID vehicleId, UUID geofenceId, int page, int size) {
        requirePage(page, size, MAX_PAGE);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_vehicle_geofence_state WHERE tenant_id=?
                  AND stable_state IS NOT NULL
                  AND (CAST(? AS uuid) IS NULL OR vehicle_id=CAST(? AS uuid))
                  AND (CAST(? AS uuid) IS NULL OR geofence_id=CAST(? AS uuid))
                ORDER BY geofence_id,vehicle_id LIMIT ? OFFSET ?
                """, this::mapState, tenantId, vehicleId, vehicleId, geofenceId, geofenceId,
                size, page * size));
    }

    @Override
    public long count(UUID tenantId, UUID vehicleId, UUID geofenceId) {
        Long count = jdbc.queryForObject("""
                SELECT count(*) FROM tracking_vehicle_geofence_state WHERE tenant_id=?
                  AND stable_state IS NOT NULL
                  AND (CAST(? AS uuid) IS NULL OR vehicle_id=CAST(? AS uuid))
                  AND (CAST(? AS uuid) IS NULL OR geofence_id=CAST(? AS uuid))
                """, Long.class, tenantId, vehicleId, vehicleId, geofenceId, geofenceId);
        return count == null ? 0 : count;
    }

    @Override
    public Claim claim(UUID tenantId, String scope, String key, String requestHash,
                       UUID targetId, UUID actorId, Instant now) {
        UUID claimId = UUID.nameUUIDFromBytes((tenantId + "|GEOFENCE|" + scope + "|" + key)
                .getBytes(StandardCharsets.UTF_8));
        String detail = "HASH=" + requestHash + ";VERSION=PENDING";
        int inserted = jdbc.update("""
                INSERT INTO tracking_audit_event(
                 id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at)
                VALUES(?, ?, ?, 'GEOFENCE_COMMAND_CLAIMED', ?, ?, ?, ?)
                ON CONFLICT(id) DO NOTHING
                """, claimId, tenantId, actorId, "GEOFENCE_" + scope, targetId, detail,
                timestamp(now));
        if (inserted == 1) {
            return new Claim(claimId, targetId, requestHash, null, true);
        }
        return jdbc.query("""
                SELECT target_id,safe_detail FROM tracking_audit_event
                WHERE id=? AND tenant_id=? AND action='GEOFENCE_COMMAND_CLAIMED'
                """, row -> {
                    if (!row.next()) {
                        throw new BusinessRuleException(
                                "IDEMPOTENCY_KEY_CONFLICT", "Idempotency key is unavailable");
                    }
                    String stored = row.getString("safe_detail");
                    String hash = stored.substring(5, stored.indexOf(';'));
                    String version = stored.substring(stored.indexOf("VERSION=") + 8);
                    Long resultVersion = "PENDING".equals(version) ? null : Long.valueOf(version);
                    return new Claim(claimId, row.getObject("target_id", UUID.class), hash,
                            resultVersion, false);
                }, claimId, tenantId);
    }

    @Override
    public void complete(UUID claimId, long resultVersion) {
        int changed = jdbc.update("""
                UPDATE tracking_audit_event
                SET safe_detail=regexp_replace(safe_detail,'VERSION=[^;]+','VERSION=' || ?)
                WHERE id=? AND action='GEOFENCE_COMMAND_CLAIMED'
                """, Long.toString(resultVersion), claimId);
        if (changed != 1) {
            throw new IllegalStateException("Geofence command claim is unavailable");
        }
    }

    @Override
    public void audit(UUID tenantId, UUID actorId, String action, UUID geofenceId,
                      String safeDetail, Instant now) {
        jdbc.update("""
                INSERT INTO tracking_audit_event(
                 id,tenant_id,actor_id,action,target_type,target_id,safe_detail,occurred_at)
                VALUES(?,?,?,?,'GEOFENCE',?,?,?)
                """, UUID.randomUUID(), tenantId, actorId, action, geofenceId, safeDetail,
                timestamp(now));
    }

    @Override
    public GeofenceTransition append(GeofenceTransition transition) {
        try {
            jdbc.update("""
                    INSERT INTO tracking_geofence_transition(
                     id,tenant_id,geofence_id,vehicle_id,location_id,geofence_type,transition,
                     severity,source_timestamp,definition_version,confirming_position_id,
                     from_state,to_state,transition_identity,created_at)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,now()) ON CONFLICT DO NOTHING
                    """, transition.transitionId(), transition.tenantId(), transition.geofenceId(),
                    transition.vehicleId(), transition.locationId(), transition.geofenceType().name(),
                    transition.transitionType().name(), transition.severity().name(),
                    timestamp(transition.sourceTimestamp()), transition.definitionVersion(),
                    transition.confirmingPositionId(), transition.fromState().name(),
                    transition.toState().name(), transition.transitionId());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException("GEOFENCE_TRANSITION_INVALID",
                    "Geofence transition could not be persisted");
        }
        return findByIdentity(transition.tenantId(), transition.transitionId()).orElseThrow();
    }

    @Override
    public Optional<GeofenceTransition> findByIdentity(UUID tenantId, UUID transitionId) {
        return jdbc.query("""
                SELECT * FROM tracking_geofence_transition
                WHERE tenant_id=? AND transition_identity=?
                """, this::mapTransition, tenantId, transitionId).stream().findFirst();
    }

    @Override
    public List<GeofenceTransition> find(
            UUID tenantId, UUID geofenceId, UUID vehicleId, GeofenceType type,
            Instant from, Instant to, String cursor, int limit, boolean unauthorizedOnly) {
        requireLimit(limit, MAX_TRANSITION_PAGE);
        Cursor parsed = Cursor.parse(cursor);
        return List.copyOf(jdbc.query("""
                SELECT * FROM tracking_geofence_transition WHERE tenant_id=?
                  AND (CAST(? AS uuid) IS NULL OR geofence_id=CAST(? AS uuid))
                  AND (CAST(? AS uuid) IS NULL OR vehicle_id=CAST(? AS uuid))
                  AND (CAST(? AS varchar) IS NULL OR geofence_type=?)
                  AND (CAST(? AS timestamptz) IS NULL OR source_timestamp>=CAST(? AS timestamptz))
                  AND (CAST(? AS timestamptz) IS NULL OR source_timestamp<=CAST(? AS timestamptz))
                  AND (?=false OR transition='UNAUTHORIZED_ZONE_ENTERED')
                  AND (CAST(? AS timestamptz) IS NULL OR source_timestamp<CAST(? AS timestamptz)
                       OR (source_timestamp=CAST(? AS timestamptz) AND id<CAST(? AS uuid)))
                ORDER BY source_timestamp DESC,id DESC LIMIT ?
                """, this::mapTransition, tenantId, geofenceId, geofenceId, vehicleId, vehicleId,
                name(type), name(type), timestamp(from), timestamp(from), timestamp(to), timestamp(to),
                unauthorizedOnly, timestamp(parsed.time()), timestamp(parsed.time()),
                timestamp(parsed.time()), parsed.id(), limit));
    }

    private Optional<VehicleGeofenceState> findState(
            UUID tenantId, UUID geofenceId, UUID vehicleId) {
        return jdbc.query("""
                SELECT * FROM tracking_vehicle_geofence_state
                WHERE tenant_id=? AND geofence_id=? AND vehicle_id=?
                """, this::mapState, tenantId, geofenceId, vehicleId).stream().findFirst();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Geofence mapGeofence(ResultSet row, int rowNumber) throws SQLException {
        return new Geofence(uuid(row, "id"), uuid(row, "tenant_id"), row.getString("name"),
                GeofenceType.valueOf(row.getString("type")), polygon(row.getString("polygon_vertices")),
                nullableUuid(row, "location_id"),
                new GeofenceAlertPolicy(row.getBoolean("alert_enter_enabled"),
                        row.getBoolean("alert_exit_enabled")),
                GeofenceLifecycle.valueOf(row.getString("lifecycle")), row.getLong("version"),
                instant(row, "created_at"), uuid(row, "created_by"), instant(row, "updated_at"),
                uuid(row, "updated_by"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private VehicleGeofenceState mapState(ResultSet row, int rowNumber) throws SQLException {
        return new VehicleGeofenceState(uuid(row, "tenant_id"), uuid(row, "geofence_id"),
                uuid(row, "vehicle_id"), row.getLong("definition_version"),
                membership(row.getString("stable_state")), membership(row.getString("pending_candidate")),
                row.getInt("pending_count"), nullableUuid(row, "pending_position_id"),
                nullableUuid(row, "last_evaluated_position_id"),
                instant(row, "last_evaluated_source_timestamp"), row.getLong("version"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private GeofenceTransition mapTransition(ResultSet row, int rowNumber) throws SQLException {
        return new GeofenceTransition(uuid(row, "id"), uuid(row, "tenant_id"),
                uuid(row, "geofence_id"), uuid(row, "vehicle_id"), nullableUuid(row, "location_id"),
                GeofenceType.valueOf(row.getString("geofence_type")),
                GeofenceTransitionType.valueOf(row.getString("transition")),
                GeofenceSeverity.valueOf(row.getString("severity")), instant(row, "source_timestamp"),
                row.getLong("definition_version"), uuid(row, "confirming_position_id"),
                GeofenceMembership.valueOf(row.getString("from_state")),
                GeofenceMembership.valueOf(row.getString("to_state")));
    }

    private String serialize(GeofencePolygon polygon) {
        try {
            String value = json.writeValueAsString(polygon.closedRing());
            if (value.getBytes(StandardCharsets.UTF_8).length > MAX_POLYGON_BYTES) {
                throw new BusinessRuleException(
                        "GEOFENCE_INVALID_GEOMETRY", "Serialized polygon exceeds 16 KiB");
            }
            return value;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Geofence polygon serialization failed", exception);
        }
    }

    private GeofencePolygon polygon(String value) {
        try {
            List<Wgs84Coordinate> closed = json.readValue(
                    value, new TypeReference<List<Wgs84Coordinate>>() { });
            if (closed.size() < 4 || !closed.getFirst().equals(closed.getLast())) {
                throw new IllegalStateException("Stored geofence polygon is not a canonical closed ring");
            }
            return GeofencePolygon.of(new ArrayList<>(closed.subList(0, closed.size() - 1)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored geofence polygon is invalid", exception);
        }
    }

    private static void requirePage(int page, int size, int maximum) {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }
        requireLimit(size, maximum);
    }

    private static void requireLimit(int limit, int maximum) {
        if (limit < 1 || limit > maximum) {
            throw new IllegalArgumentException("Limit must be between 1 and " + maximum);
        }
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static GeofenceMembership membership(String value) {
        return value == null ? null : GeofenceMembership.valueOf(value);
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static UUID uuid(ResultSet row, String column) throws SQLException {
        return UUID.fromString(row.getString(column));
    }

    private static UUID nullableUuid(ResultSet row, String column) throws SQLException {
        String value = row.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static BusinessRuleException stale() {
        return new BusinessRuleException("GEOFENCE_STALE_VERSION",
                "Geofence persistence version is stale or resource is unavailable");
    }

    private static BusinessRuleException conflict() {
        return new BusinessRuleException("GEOFENCE_NAME_CONFLICT",
                "Geofence definition conflicts with existing Tenant data");
    }

    private record Cursor(Instant time, UUID id) {
        private static Cursor parse(String value) {
            if (value == null || value.isBlank()) {
                return new Cursor(null, null);
            }
            try {
                String[] facts = value.split("\\|", -1);
                if (facts.length != 2) {
                    throw new IllegalArgumentException();
                }
                return new Cursor(Instant.parse(facts[0]), UUID.fromString(facts[1]));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Transition cursor is invalid", exception);
            }
        }
    }
}
