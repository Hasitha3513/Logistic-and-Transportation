package com.transportlogistics.app.fuel.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface FuelLimitPolicyJpaRepository extends JpaRepository<FuelLimitPolicyEntity, UUID> {
    @Query("select policy from FuelLimitPolicyEntity policy where policy.active = true and (policy.vehicleId = :vehicleId or policy.vehicleId is null)")
    List<FuelLimitPolicyEntity> findApplicable(@Param("vehicleId") UUID vehicleId);
}
