package com.transportlogistics.app.freight.insurance.adapters.inbound.web.mappers;

import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.response.ClaimSettlementResponse;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.response.FreightInsuranceClaimResponse;
import com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.response.FreightInsurancePolicyResponse;
import com.transportlogistics.app.freight.insurance.domain.ClaimSettlement;
import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.domain.PolicyStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class FreightInsuranceWebMapper {

    public FreightInsurancePolicyResponse toResponse(FreightInsurancePolicy policy) {
        if (policy == null) return null;
        return new FreightInsurancePolicyResponse(
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getFreightOrderId(),
                policy.getCargoManifestId(),
                policy.getInsuranceProvider(),
                policy.getPolicyType(),
                policy.getCoverageAmount(),
                policy.getPremiumAmount(),
                policy.getCurrency(),
                policy.getValidFrom(),
                policy.getValidUntil(),
                policy.getStatus().name(),
                policy.isValidAt(OffsetDateTime.now()),
                policy.getVersion(),
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                policy.getCreatedBy(),
                policy.getUpdatedBy()
        );
    }

    public ClaimSettlementResponse toResponse(ClaimSettlement settlement) {
        if (settlement == null) return null;
        return new ClaimSettlementResponse(
                settlement.id(),
                settlement.claimId(),
                settlement.settlementReference(),
                settlement.amount(),
                settlement.currency(),
                settlement.notes(),
                settlement.settledBy(),
                settlement.settledAt()
        );
    }

    public FreightInsuranceClaimResponse toResponse(FreightInsuranceClaim claim) {
        if (claim == null) return null;
        List<ClaimSettlementResponse> settlements = claim.getSettlements() == null
                ? List.of()
                : claim.getSettlements().stream().map(this::toResponse).toList();

        return new FreightInsuranceClaimResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getPolicyId(),
                claim.getFreightOrderId(),
                claim.getIncidentReference(),
                claim.getDamageDescription(),
                claim.getClaimedAmount(),
                claim.getAssessedAmount(),
                claim.getAssessmentNotes(),
                claim.getAssessedBy(),
                claim.getAssessedAt(),
                claim.getStatus().name(),
                claim.getResolutionReason(),
                claim.getTotalSettledAmount(),
                claim.getRemainingApprovedAmount(),
                settlements,
                claim.getVersion(),
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                claim.getCreatedBy(),
                claim.getUpdatedBy()
        );
    }
}
