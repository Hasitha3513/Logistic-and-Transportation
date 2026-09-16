package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Effective-dated, Tenant-qualified capability lookup; persistence is a later governed slice. */
public interface TelemetryCapabilityLookupPort {
    TelemetryCapabilityState resolve(
            UUID tenantId, UUID deviceId, TelemetrySignalCapability capability, Instant sourceTimestamp);

    default Map<TelemetrySignalCapability, TelemetryCapabilityState> resolveAll(
            UUID tenantId, UUID deviceId, Set<TelemetrySignalCapability> capabilities,
            Instant sourceTimestamp) {
        java.util.EnumMap<TelemetrySignalCapability, TelemetryCapabilityState> result =
                new java.util.EnumMap<>(TelemetrySignalCapability.class);
        capabilities.forEach(capability -> result.put(capability,
                resolve(tenantId, deviceId, capability, sourceTimestamp)));
        return Map.copyOf(result);
    }
}
