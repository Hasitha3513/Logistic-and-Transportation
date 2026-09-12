package com.transportlogistics.app.trip.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.trip.VehicleTripAssignmentLookup;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcVehicleTripAssignmentLookupAdapter implements VehicleTripAssignmentLookup {
    private final JdbcTemplate jdbc;

    JdbcVehicleTripAssignmentLookupAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<VehicleTripAssignment> findAt(
            UUID tenantId, UUID vehicleId, Instant sourceTimestamp) {
        return jdbc.query("""
                SELECT id,driver_id,route_id FROM trip
                WHERE tenant_id=? AND vehicle_id=? AND actual_start_time IS NOT NULL
                  AND actual_start_time<=? AND (actual_end_time IS NULL OR actual_end_time>=?)
                  AND status NOT IN('CANCELLED','REJECTED')
                ORDER BY actual_start_time DESC,id DESC LIMIT 1
                """, this::map, tenantId, vehicleId, Timestamp.from(sourceTimestamp),
                Timestamp.from(sourceTimestamp)).stream().findFirst();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private VehicleTripAssignment map(ResultSet row, int rowNumber) throws SQLException {
        return new VehicleTripAssignment(row.getObject("id", UUID.class),
                row.getObject("driver_id", UUID.class), row.getObject("route_id", UUID.class), null);
    }
}
