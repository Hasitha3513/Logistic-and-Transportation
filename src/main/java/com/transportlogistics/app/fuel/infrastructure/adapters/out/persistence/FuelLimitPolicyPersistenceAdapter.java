package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fuel.application.ports.out.FuelLimitPolicyRepository;
import com.transportlogistics.app.fuel.domain.model.FuelLimitPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class FuelLimitPolicyPersistenceAdapter implements FuelLimitPolicyRepository {
    private final FuelLimitPolicyJpaRepository repository;

    @Override
    public List<FuelLimitPolicy> findApplicable(UUID vehicleId) {
        return repository.findApplicable(vehicleId).stream().map(entity -> new FuelLimitPolicy(entity.getId(),
                entity.getVehicleId(), entity.getMaximumQuantityPerIssue(), entity.isActive())).toList();
    }
}
