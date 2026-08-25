package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import com.transportlogistics.app.freight.insurance.domain.ClaimSettlement;
import com.transportlogistics.app.freight.insurance.domain.ClaimStatus;
import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.domain.PolicyStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FreightInsurancePersistenceMapper {

    public FreightInsurancePolicyEntity toEntity(FreightInsurancePolicy domain) {
        FreightInsurancePolicyEntity entity = new FreightInsurancePolicyEntity();
        entity.setId(domain.getId());
        entity.setPolicyNumber(domain.getPolicyNumber());
        entity.setFreightOrderId(domain.getFreightOrderId());
        entity.setCargoManifestId(domain.getCargoManifestId());
        entity.setInsuranceProvider(domain.getInsuranceProvider());
        entity.setPolicyType(domain.getPolicyType());
        entity.setCoverageAmount(domain.getCoverageAmount());
        entity.setPremiumAmount(domain.getPremiumAmount());
        entity.setCurrency(domain.getCurrency());
        entity.setValidFrom(domain.getValidFrom());
        entity.setValidUntil(domain.getValidUntil());
        entity.setStatus(domain.getStatus().name());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedBy(domain.getUpdatedBy());
        return entity;
    }

    public FreightInsurancePolicy toDomain(FreightInsurancePolicyEntity entity) {
        return new FreightInsurancePolicy(
                entity.getId(),
                entity.getPolicyNumber(),
                entity.getFreightOrderId(),
                entity.getCargoManifestId(),
                entity.getInsuranceProvider(),
                entity.getPolicyType(),
                entity.getCoverageAmount(),
                entity.getPremiumAmount(),
                entity.getCurrency(),
                entity.getValidFrom(),
                entity.getValidUntil(),
                PolicyStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getVersion()
        );
    }

    public FreightInsuranceClaimEntity toEntity(FreightInsuranceClaim domain) {
        FreightInsuranceClaimEntity entity = new FreightInsuranceClaimEntity();
        entity.setId(domain.getId());
        entity.setClaimNumber(domain.getClaimNumber());
        entity.setPolicyId(domain.getPolicyId());
        entity.setFreightOrderId(domain.getFreightOrderId());
        entity.setIncidentReference(domain.getIncidentReference());
        entity.setDamageDescription(domain.getDamageDescription());
        entity.setClaimedAmount(domain.getClaimedAmount());
        entity.setAssessedAmount(domain.getAssessedAmount());
        entity.setAssessmentNotes(domain.getAssessmentNotes());
        entity.setAssessedBy(domain.getAssessedBy());
        entity.setAssessedAt(domain.getAssessedAt());
        entity.setStatus(domain.getStatus().name());
        entity.setResolutionReason(domain.getResolutionReason());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedBy(domain.getUpdatedBy());

        List<FreightInsuranceSettlementEntity> settlements = new ArrayList<>();
        if (domain.getSettlements() != null) {
            for (ClaimSettlement s : domain.getSettlements()) {
                FreightInsuranceSettlementEntity sEntity = new FreightInsuranceSettlementEntity();
                sEntity.setId(s.id());
                sEntity.setClaim(entity);
                sEntity.setSettlementReference(s.settlementReference());
                sEntity.setAmount(s.amount());
                sEntity.setCurrency(s.currency());
                sEntity.setNotes(s.notes());
                sEntity.setSettledBy(s.settledBy());
                sEntity.setSettledAt(s.settledAt());
                settlements.add(sEntity);
            }
        }
        entity.setSettlements(settlements);
        return entity;
    }

    public FreightInsuranceClaim toDomain(FreightInsuranceClaimEntity entity) {
        List<ClaimSettlement> settlements = entity.getSettlements().stream()
                .map(s -> new ClaimSettlement(
                        s.getId(),
                        entity.getId(),
                        s.getSettlementReference(),
                        s.getAmount(),
                        s.getCurrency(),
                        s.getNotes(),
                        s.getSettledBy(),
                        s.getSettledAt()
                ))
                .toList();

        return new FreightInsuranceClaim(
                entity.getId(),
                entity.getClaimNumber(),
                entity.getPolicyId(),
                entity.getFreightOrderId(),
                entity.getIncidentReference(),
                entity.getDamageDescription(),
                entity.getClaimedAmount(),
                entity.getAssessedAmount(),
                entity.getAssessmentNotes(),
                entity.getAssessedBy(),
                entity.getAssessedAt(),
                ClaimStatus.valueOf(entity.getStatus()),
                entity.getResolutionReason(),
                settlements,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getVersion()
        );
    }
}
