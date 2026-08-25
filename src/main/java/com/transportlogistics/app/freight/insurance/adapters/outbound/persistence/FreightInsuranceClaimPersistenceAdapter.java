package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.ports.outbound.FreightInsuranceClaimRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FreightInsuranceClaimPersistenceAdapter implements FreightInsuranceClaimRepository {

    private final FreightInsuranceClaimJpaRepository claimRepository;
    private final FreightInsurancePersistenceMapper mapper;

    public FreightInsuranceClaimPersistenceAdapter(FreightInsuranceClaimJpaRepository claimRepository,
                                                  FreightInsurancePersistenceMapper mapper) {
        this.claimRepository = claimRepository;
        this.mapper = mapper;
    }

    @Override
    public FreightInsuranceClaim save(FreightInsuranceClaim claim) {
        Optional<FreightInsuranceClaimEntity> existingOpt = claimRepository.findById(claim.getId());
        FreightInsuranceClaimEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setAssessedAmount(claim.getAssessedAmount());
            entity.setAssessmentNotes(claim.getAssessmentNotes());
            entity.setAssessedBy(claim.getAssessedBy());
            entity.setAssessedAt(claim.getAssessedAt());
            entity.setStatus(claim.getStatus().name());
            entity.setResolutionReason(claim.getResolutionReason());
            entity.setUpdatedAt(claim.getUpdatedAt());
            entity.setUpdatedBy(claim.getUpdatedBy());
            entity.setVersion(claim.getVersion());

            FreightInsuranceClaimEntity newMapped = mapper.toEntity(claim);
            entity.replaceSettlements(newMapped.getSettlements());
        } else {
            entity = mapper.toEntity(claim);
        }
        FreightInsuranceClaimEntity saved = claimRepository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<FreightInsuranceClaim> findById(UUID id) {
        return claimRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<FreightInsuranceClaim> findAll() {
        return claimRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<FreightInsuranceClaim> findByPolicyId(UUID policyId) {
        return claimRepository.findByPolicyId(policyId).stream().map(mapper::toDomain).toList();
    }
}
