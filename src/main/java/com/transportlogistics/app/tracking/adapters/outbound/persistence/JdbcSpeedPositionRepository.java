package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.domain.speed.SpeedKph;
import com.transportlogistics.app.tracking.domain.speed.SpeedPosition;
import com.transportlogistics.app.tracking.ports.outbound.SpeedPositionRepositoryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcSpeedPositionRepository implements SpeedPositionRepositoryPort {
    private final JdbcTemplate jdbc;

    JdbcSpeedPositionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SpeedPosition> find(UUID tenantId, UUID positionId) {
        return jdbc.query("""
                SELECT tenant_id,id,vehicle_id,source_timestamp,speed_kph,trust,ordering_classification
                FROM tracking_position WHERE tenant_id=? AND id=?
                """, this::map, tenantId, positionId).stream().findFirst();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private SpeedPosition map(ResultSet row, int rowNumber) throws SQLException {
        var speed = row.getBigDecimal("speed_kph");
        return new SpeedPosition(row.getObject("tenant_id", UUID.class),
                row.getObject("vehicle_id", UUID.class), row.getObject("id", UUID.class),
                row.getTimestamp("source_timestamp").toInstant(),
                speed == null ? null : new SpeedKph(speed), false,
                "TRUSTED".equals(row.getString("trust")), true,
                "IN_ORDER".equals(row.getString("ordering_classification")));
    }
}
