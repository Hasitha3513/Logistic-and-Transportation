package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.ports.outbound.GpsDeviceFreshnessPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.tracking.hybrid-storage.enabled", havingValue = "true")
final class JdbcGpsDeviceFreshnessAdapter implements GpsDeviceFreshnessPort {
    private final JdbcTemplate jdbc;

    JdbcGpsDeviceFreshnessAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<DeviceFreshness> findOfflineCandidates(Instant receivedBefore, int limit) {
        if (receivedBefore == null || limit < 1 || limit > 500) {
            throw new IllegalArgumentException("GPS freshness query is invalid");
        }
        return List.copyOf(jdbc.query("""
                SELECT device.tenant_id,device.id,assignment.vehicle_id,latest.received_at
                FROM tracking_device device
                JOIN tracking_vehicle_device_assignment assignment
                  ON assignment.tenant_id=device.tenant_id
                 AND assignment.tracking_device_id=device.id
                 AND assignment.effective_to IS NULL
                JOIN LATERAL (
                  SELECT history.received_at
                  FROM tracking_position_history history
                  WHERE history.tenant_id=device.tenant_id AND history.device_id=device.id
                  ORDER BY history.source_timestamp DESC,history.id DESC LIMIT 1
                ) latest ON true
                WHERE device.lifecycle='ACTIVE' AND latest.received_at<?
                ORDER BY latest.received_at,device.tenant_id,device.id LIMIT ?
                """, (row, number) -> new DeviceFreshness(
                        java.util.UUID.fromString(row.getString(1)),
                        java.util.UUID.fromString(row.getString(2)),
                        java.util.UUID.fromString(row.getString(3)),
                        row.getTimestamp(4).toInstant()),
                Timestamp.from(receivedBefore), limit));
    }
}
