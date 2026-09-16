package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.Incident;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.IncidentType;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.ProducerStatus;
import com.transportlogistics.app.tracking.domain.dashboard.TrackingDashboardModels.SourceStatus;
import com.transportlogistics.app.tracking.ports.outbound.TrackingDashboardIncidentPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class TrackingDashboardIncidentAdapter implements TrackingDashboardIncidentPort {
    private final NamedParameterJdbcTemplate jdbc;

    TrackingDashboardIncidentAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public IncidentResult find(
            UUID tenantId, Set<UUID> vehicleIds, Set<IncidentType> types,
            Instant fromInclusive, Instant toExclusive, int maximumPerProducer, int maximumTotal) {
        Set<IncidentType> requested = types.isEmpty() ? Set.of(IncidentType.values()) : Set.copyOf(types);
        List<Incident> incidents = new ArrayList<>();
        var statuses = new EnumMap<IncidentType, SourceStatus>(IncidentType.class);
        for (IncidentType type : requested) {
            try {
                incidents.addAll(query(type, tenantId, vehicleIds, fromInclusive,
                        toExclusive, maximumPerProducer));
                statuses.put(type, SourceStatus.AVAILABLE);
            } catch (DataAccessException exception) {
                statuses.put(type, SourceStatus.UNAVAILABLE);
            }
        }
        List<Incident> result = incidents.stream()
                .sorted(Comparator.comparing(Incident::sourceTimestamp).reversed()
                        .thenComparing(Incident::evidenceId, Comparator.reverseOrder()))
                .limit(maximumTotal)
                .toList();
        return new IncidentResult(result, statuses);
    }

    private List<Incident> query(
            IncidentType type, UUID tenantId, Set<UUID> vehicleIds,
            Instant fromInclusive, Instant toExclusive, int limit) {
        var parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("fromInclusive", Timestamp.from(fromInclusive))
                .addValue("toExclusive", Timestamp.from(toExclusive))
                .addValue("limit", limit);
        String vehiclePredicate = "";
        if (!vehicleIds.isEmpty()) {
            parameters.addValue("vehicleIds", vehicleIds);
            vehiclePredicate = " AND vehicle_id IN (:vehicleIds)";
        }
        return switch (type) {
            case GEOFENCE -> jdbc.query("""
                    SELECT id, vehicle_id, NULL::uuid AS trip_id, NULL::uuid AS route_id,
                           NULL::varchar AS route_version, severity, transition AS status,
                           source_timestamp
                    FROM tracking_geofence_transition
                    WHERE tenant_id = :tenantId
                      AND source_timestamp >= :fromInclusive AND source_timestamp < :toExclusive
                    """ + vehiclePredicate
                    + " ORDER BY source_timestamp DESC, id DESC LIMIT :limit",
                    parameters, (row, number) -> map(row, type, ProducerStatus.ACCEPTED));
            case SPEED -> jdbc.query("""
                    SELECT id, vehicle_id, trip_id, route_id, route_version, severity,
                           CASE WHEN end_source_timestamp IS NULL THEN 'OPEN' ELSE 'CLOSED' END AS status,
                           confirmation_source_timestamp AS source_timestamp
                    FROM tracking_speed_episode
                    WHERE tenant_id = :tenantId
                      AND confirmation_source_timestamp >= :fromInclusive
                      AND confirmation_source_timestamp < :toExclusive
                    """ + vehiclePredicate
                    + " ORDER BY confirmation_source_timestamp DESC, id DESC LIMIT :limit",
                    parameters, (row, number) -> map(row, type, ProducerStatus.FIELD_FIDELITY_PENDING));
            case ROUTE_DEVIATION -> jdbc.query("""
                    SELECT id, vehicle_id, trip_id, route_id, route_version, severity,
                           review_status AS status, confirmation_source_timestamp AS source_timestamp
                    FROM tracking_route_deviation_episode
                    WHERE tenant_id = :tenantId
                      AND confirmation_source_timestamp >= :fromInclusive
                      AND confirmation_source_timestamp < :toExclusive
                    """ + vehiclePredicate
                    + " ORDER BY confirmation_source_timestamp DESC, id DESC LIMIT :limit",
                    parameters, (row, number) -> map(row, type, ProducerStatus.FIELD_ACCEPTANCE_PENDING));
        };
    }

    private Incident map(ResultSet row, IncidentType type, ProducerStatus producerStatus)
            throws SQLException {
        return new Incident(row.getObject("id", UUID.class), type,
                row.getObject("vehicle_id", UUID.class), row.getObject("trip_id", UUID.class),
                row.getObject("route_id", UUID.class), parseVersion(row.getString("route_version")),
                row.getString("severity"), row.getString("status"),
                row.getTimestamp("source_timestamp").toInstant(), producerStatus);
    }

    private static Long parseVersion(String value) {
        if (value == null) {
            return null;
        }
        String numeric = value.startsWith("REVISION:") ? value.substring("REVISION:".length()) : value;
        try {
            return Long.valueOf(numeric);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
