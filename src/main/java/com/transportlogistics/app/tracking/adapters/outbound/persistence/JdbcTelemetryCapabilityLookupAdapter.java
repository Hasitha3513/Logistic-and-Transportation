package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryCapabilityLookupPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Tenant-qualified source-time capability registry lookup. */
@Component
final class JdbcTelemetryCapabilityLookupAdapter implements TelemetryCapabilityLookupPort {
    private final JdbcTemplate jdbc;

    JdbcTelemetryCapabilityLookupAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TelemetryCapabilityState resolve(
            UUID tenantId,
            UUID deviceId,
            TelemetrySignalCapability capability,
            Instant sourceTimestamp) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(sourceTimestamp, "sourceTimestamp");
        return jdbc.query("""
                SELECT capability_state
                FROM tracking_device_telemetry_capability
                WHERE tenant_id=? AND tracking_device_id=? AND capability=?
                  AND effective_from<=?
                  AND (effective_to IS NULL OR effective_to>?)
                ORDER BY effective_from DESC,id DESC
                LIMIT 1
                """, (row, number) -> TelemetryCapabilityState.valueOf(row.getString(1)),
                tenantId, deviceId, capability.name(), Timestamp.from(sourceTimestamp),
                Timestamp.from(sourceTimestamp)).stream().findFirst()
                .orElse(TelemetryCapabilityState.UNKNOWN);
    }
}
