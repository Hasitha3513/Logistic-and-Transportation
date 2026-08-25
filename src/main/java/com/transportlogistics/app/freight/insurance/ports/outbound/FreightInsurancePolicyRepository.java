package com.transportlogistics.app.freight.insurance.ports.outbound;

import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FreightInsurancePolicyRepository {

    FreightInsurancePolicy save(FreightInsurancePolicy policy);

    Optional<FreightInsurancePolicy> findById(UUID id);

    Optional<FreightInsurancePolicy> findByFreightOrderId(UUID freightOrderId);

    List<FreightInsurancePolicy> findAll();
}
