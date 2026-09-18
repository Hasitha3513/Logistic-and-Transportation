package com.transportlogistics.app.fleet.vehiclemaster.adapters.outbound;

import com.transportlogistics.app.fleet.VehiclePowertrainEligibilityQuery;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Fail-closed until Fleet owns an authoritative effective-dated powertrain fact. */
@Component
final class VehiclePowertrainEligibilityAdapter implements VehiclePowertrainEligibilityQuery {
    @Override
    public Classification classify(UUID tenantId, UUID vehicleId, Instant observedAt) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(observedAt);
        return Classification.UNKNOWN;
    }
}
