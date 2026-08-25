package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsurancePolicyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FreightInsurancePolicyPersistenceAdapter implements FreightInsurancePolicyRepository {

    private final FreightInsurancePolicyJpaRepository policyRepository;
    private final FreightInsurancePersistenceMapper mapper;

    public FreightInsurancePolicyPersistenceAdapter(FreightInsurancePolicyJpaRepository policyRepository,
                                                   FreightInsurancePersistenceMapper mapper) {
        this.policyRepository = policyRepository;
        this.mapper = mapper;
    }

    @Override
    public FreightInsurancePolicy save(FreightInsurancePolicy policy) {
        FreightInsurancePolicyEntity entity = mapper.toEntity(policy);
        FreightInsurancePolicyEntity saved = policyRepository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<FreightInsurancePolicy> findById(UUID id) {
        return policyRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<FreightInsurancePolicy> findByFreightOrderId(UUID freightOrderId) {
        return policyRepository.findByFreightOrderId(freightOrderId).map(mapper::toDomain);
    }

    @Override
    public List<FreightInsurancePolicy> findAll() {
        return policyRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
