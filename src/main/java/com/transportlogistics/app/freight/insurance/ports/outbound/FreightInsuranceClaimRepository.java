package com.transportlogistics.app.freight.insurance.ports.outbound;

import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FreightInsuranceClaimRepository {

    FreightInsuranceClaim save(FreightInsuranceClaim claim);

    Optional<FreightInsuranceClaim> findById(UUID id);

    List<FreightInsuranceClaim> findAll();

    List<FreightInsuranceClaim> findByPolicyId(UUID policyId);
}
