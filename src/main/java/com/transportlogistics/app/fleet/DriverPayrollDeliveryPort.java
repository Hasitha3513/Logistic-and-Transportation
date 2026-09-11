package com.transportlogistics.app.fleet;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Internal Driver-owned delivery observation for the accepted Integration file result. */
public interface DriverPayrollDeliveryPort {
    void fileDelivered(UUID tenantId, UUID sourceEventId, String payloadHash,
                       String targetFilename, OffsetDateTime deliveredAt);
}
