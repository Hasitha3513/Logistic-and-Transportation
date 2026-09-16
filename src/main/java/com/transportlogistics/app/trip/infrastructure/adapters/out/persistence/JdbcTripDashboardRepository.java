package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.TripDashboardQuery.ActiveTripContext;
import com.transportlogistics.app.trip.application.ports.out.TripDashboardRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class JdbcTripDashboardRepository implements TripDashboardRepository {
    private final NamedParameterJdbcTemplate jdbc;

    JdbcTripDashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ActiveTripContext> findActiveContexts(
            UUID tenantId, Set<UUID> vehicleIds, Instant evaluatedAt) {
        return List.copyOf(jdbc.query("""
                SELECT DISTINCT ON (vehicle_id)
                       vehicle_id, id, status, route_id, route_version
                FROM trip
                WHERE tenant_id = :tenantId
                  AND vehicle_id IN (:vehicleIds)
                  AND actual_start_time IS NOT NULL
                  AND actual_start_time <= :evaluatedAt
                  AND (actual_end_time IS NULL OR actual_end_time > :evaluatedAt)
                  AND status NOT IN ('CANCELLED', 'REJECTED')
                ORDER BY vehicle_id, actual_start_time DESC, id DESC
                """, Map.of("tenantId", tenantId, "vehicleIds", vehicleIds,
                        "evaluatedAt", Timestamp.from(evaluatedAt)), (row, number) ->
                        new ActiveTripContext(row.getObject("vehicle_id", UUID.class),
                                row.getObject("id", UUID.class), row.getString("status"),
                                row.getObject("route_id", UUID.class), row.getString("route_version"))));
    }
}
