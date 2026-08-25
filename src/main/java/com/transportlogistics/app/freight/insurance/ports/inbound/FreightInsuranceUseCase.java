package com.transportlogistics.app.freight.insurance.ports.inbound;

import com.transportlogistics.app.freight.insurance.domain.FreightInsuranceClaim;
import com.transportlogistics.app.freight.insurance.domain.FreightInsurancePolicy;
import com.transportlogistics.app.freight.insurance.domain.PolicyStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FreightInsuranceUseCase {

    record AssociatePolicyCommand(
            UUID freightOrderId,
            UUID cargoManifestId,
            String insuranceProvider,
            String policyType,
            BigDecimal coverageAmount,
            BigDecimal premiumAmount,
            String currency,
            OffsetDateTime validFrom,
            OffsetDateTime validUntil
    ) {}

    record UpdatePolicyCommand(
            String insuranceProvider,
            String policyType,
            BigDecimal coverageAmount,
            BigDecimal premiumAmount,
            OffsetDateTime validFrom,
            OffsetDateTime validUntil,
            PolicyStatus status,
            long version
    ) {}

    record CreateClaimCommand(
            UUID policyId,
            String incidentReference,
            String damageDescription,
            BigDecimal claimedAmount
    ) {}

    record AssessClaimCommand(
            BigDecimal assessedAmount,
            String assessmentNotes,
            long version
    ) {}

    record ApproveClaimCommand(
            long version
    ) {}

    record RejectClaimCommand(
            String reason,
            long version
    ) {}

    record DisputeClaimCommand(
            String reason,
            long version
    ) {}

    record RecordSettlementCommand(
            BigDecimal amount,
            String currency,
            String settlementReference,
            String notes,
            long version
    ) {}

    FreightInsurancePolicy associatePolicy(AssociatePolicyCommand command, String actor);

    FreightInsurancePolicy getPolicy(UUID id);

    List<FreightInsurancePolicy> listPolicies();

    FreightInsurancePolicy updatePolicy(UUID id, UpdatePolicyCommand command, String actor);

    FreightInsuranceClaim createClaim(CreateClaimCommand command, String actor);

    FreightInsuranceClaim getClaim(UUID id);

    List<FreightInsuranceClaim> listClaims();

    FreightInsuranceClaim assessClaim(UUID id, AssessClaimCommand command, String actor);

    FreightInsuranceClaim approveClaim(UUID id, ApproveClaimCommand command, String actor);

    FreightInsuranceClaim rejectClaim(UUID id, RejectClaimCommand command, String actor);

    FreightInsuranceClaim disputeClaim(UUID id, DisputeClaimCommand command, String actor);

    FreightInsuranceClaim recordSettlement(UUID id, RecordSettlementCommand command, String actor);
}
