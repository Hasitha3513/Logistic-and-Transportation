package com.transportlogistics.app.tracking.ports.outbound;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GpsDeviceFreshnessPort {
    List<DeviceFreshness> findOfflineCandidates(Instant receivedBefore, int limit);

    record DeviceFreshness(
            UUID tenantId, UUID deviceId, UUID vehicleId, Instant lastReceivedAt) {
    }
}
