package com.transportlogistics.app.fuel.application.ports.out;

import com.transportlogistics.app.fuel.domain.model.FuelLimitPolicy;

import java.util.List;
import java.util.UUID;

public interface FuelLimitPolicyRepository {
    List<FuelLimitPolicy> findApplicable(UUID vehicleId);
}
