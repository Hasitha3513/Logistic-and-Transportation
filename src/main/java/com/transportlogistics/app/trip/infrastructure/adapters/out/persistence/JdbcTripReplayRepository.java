package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.TripReplayQuery.TripReplayScope;
import com.transportlogistics.app.trip.TripReplayQuery.VehicleTripAssignmentInterval;
import com.transportlogistics.app.trip.application.ports.out.TripReplayRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class JdbcTripReplayRepository implements TripReplayRepository {
    private final JdbcTemplate jdbc;

    JdbcTripReplayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<TripReplayScope> findReplayScope(UUID tenantId, UUID tripId) {
        return jdbc.query("""
                SELECT id, vehicle_id, actual_start_time, actual_end_time, status, route_id, route_version
                FROM trip
                WHERE tenant_id = ? AND id = ?
                  AND vehicle_id IS NOT NULL AND actual_start_time IS NOT NULL
                """, this::mapScope, tenantId, tripId).stream().findFirst();
    }

    @Override
    public List<VehicleTripAssignmentInterval> findAssignmentsOverlapping(
            UUID tenantId, UUID vehicleId, Instant rangeStart, Instant rangeEnd, int limit) {
        return List.copyOf(jdbc.query("""
                SELECT id, vehicle_id, actual_start_time, actual_end_time, route_id, route_version, status
                FROM trip
                WHERE tenant_id = ? AND vehicle_id = ?
                  AND actual_start_time IS NOT NULL
                  AND actual_start_time < ?
                  AND (actual_end_time IS NULL OR actual_end_time > ?)
                  AND status NOT IN ('CANCELLED', 'REJECTED')
                ORDER BY actual_start_time ASC, id ASC
                LIMIT ?
                """, this::mapInterval, tenantId, vehicleId, Timestamp.from(rangeEnd),
                Timestamp.from(rangeStart), limit));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private TripReplayScope mapScope(ResultSet row, int rowNumber) throws SQLException {
        return new TripReplayScope(row.getObject("id", UUID.class),
                row.getObject("vehicle_id", UUID.class), instant(row, "actual_start_time"),
                instant(row, "actual_end_time"), row.getString("status"),
                row.getObject("route_id", UUID.class), row.getString("route_version"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private VehicleTripAssignmentInterval mapInterval(ResultSet row, int rowNumber) throws SQLException {
        return new VehicleTripAssignmentInterval(row.getObject("id", UUID.class),
                row.getObject("vehicle_id", UUID.class), instant(row, "actual_start_time"),
                instant(row, "actual_end_time"), row.getObject("route_id", UUID.class),
                row.getString("route_version"), row.getString("status"));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
