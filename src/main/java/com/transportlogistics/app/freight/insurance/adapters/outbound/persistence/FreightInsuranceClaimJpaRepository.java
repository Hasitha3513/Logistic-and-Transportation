package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FreightInsuranceClaimJpaRepository extends JpaRepository<FreightInsuranceClaimEntity, UUID> {

    List<FreightInsuranceClaimEntity> findByPolicyId(UUID policyId);
}
