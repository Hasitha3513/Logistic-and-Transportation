package com.transportlogistics.app.tracking.adapters.outbound.persistence;

import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryCapabilityLookupPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        return resolveAll(tenantId, deviceId, Set.of(capability), sourceTimestamp).get(capability);
    }

    @Override
    public Map<TelemetrySignalCapability, TelemetryCapabilityState> resolveAll(
            UUID tenantId, UUID deviceId, Set<TelemetrySignalCapability> capabilities,
            Instant sourceTimestamp) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(sourceTimestamp, "sourceTimestamp");
        EnumMap<TelemetrySignalCapability, TelemetryCapabilityState> result =
                new EnumMap<>(TelemetrySignalCapability.class);
        capabilities.forEach(capability -> result.put(capability, TelemetryCapabilityState.UNKNOWN));
        if (capabilities.isEmpty()) {
            return Map.copyOf(result);
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(capabilities.size(), "?"));
        java.util.ArrayList<Object> parameters = new java.util.ArrayList<>();
        parameters.add(tenantId);
        parameters.add(deviceId);
        parameters.addAll(capabilities.stream().map(Enum::name).toList());
        parameters.add(Timestamp.from(sourceTimestamp));
        parameters.add(Timestamp.from(sourceTimestamp));
        jdbc.query("""
                SELECT capability,capability_state
                FROM tracking_device_telemetry_capability
                WHERE tenant_id=? AND tracking_device_id=? AND capability IN (%s)
                  AND effective_from<=?
                  AND (effective_to IS NULL OR effective_to>?)
                ORDER BY capability,effective_from DESC,id DESC
                """.formatted(placeholders), (org.springframework.jdbc.core.RowCallbackHandler) row -> result.put(
                    TelemetrySignalCapability.valueOf(row.getString(1)),
                    TelemetryCapabilityState.valueOf(row.getString(2))), parameters.toArray());
        return Map.copyOf(result);
    }
}
