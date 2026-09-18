package com.transportlogistics.app.fleet;

import java.time.Instant;
import java.util.UUID;

/** Fleet-owned, Tenant-aware source-time powertrain classification for Tracking. */
public interface VehiclePowertrainEligibilityQuery {
    Classification classify(UUID tenantId, UUID vehicleId, Instant observedAt);

    enum Classification { COMBUSTION, HYBRID, OTHER, UNKNOWN }
}
