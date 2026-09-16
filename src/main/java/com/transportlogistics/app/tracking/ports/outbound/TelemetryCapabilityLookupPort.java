package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import java.time.Instant;
import java.util.UUID;

/** Effective-dated, Tenant-qualified capability lookup; persistence is a later governed slice. */
public interface TelemetryCapabilityLookupPort {
    TelemetryCapabilityState resolve(
            UUID tenantId, UUID deviceId, TelemetrySignalCapability capability, Instant sourceTimestamp);
}
