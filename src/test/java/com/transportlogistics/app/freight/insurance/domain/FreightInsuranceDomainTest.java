package com.transportlogistics.app.freight.insurance.domain;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FreightInsuranceDomainTest {

    private final UUID policyId = UUID.randomUUID();
    private final UUID freightOrderId = UUID.randomUUID();
    private final UUID claimId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-25T10:00:00Z");
    private final OffsetDateTime validFrom = OffsetDateTime.parse("2026-08-01T00:00:00Z");
    private final OffsetDateTime validUntil = OffsetDateTime.parse("2026-08-31T23:59:59Z");

    @Test
    @DisplayName("Policy association validation and validity checks")
    void testPolicyValidation() {
        FreightInsurancePolicy policy = new FreightInsurancePolicy(
                policyId, "POL-2026-000001", freightOrderId, null,
                "Allianz Global", "ALL_RISK",
                new BigDecimal("50000.00"), new BigDecimal("1200.00"), "USD",
                validFrom, validUntil, PolicyStatus.ACTIVE,
                now, now, "manager", "manager", 0L
        );

        assertThat(policy.isValidAt(now)).isTrue();
        assertThat(policy.isValidAt(OffsetDateTime.parse("2026-09-05T00:00:00Z"))).isFalse();

        // Valid claim within coverage
        policy.validateCoverageFor(new BigDecimal("15000.00"), now);

        // Claim exceeding coverage limit
        assertThatThrownBy(() -> policy.validateCoverageFor(new BigDecimal("60000.00"), now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds policy coverage limit");

        // Expired date
        assertThatThrownBy(() -> policy.validateCoverageFor(new BigDecimal("5000.00"), OffsetDateTime.parse("2026-09-05T00:00:00Z")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Policy is not active or valid");
    }

    @Test
    @DisplayName("Claim lifecycle: create -> assess -> approve -> partial settlement -> full settlement")
    void testClaimLifecycleFullSettlement() {
        FreightInsuranceClaim claim = new FreightInsuranceClaim(
                claimId, "CLM-2026-000001", policyId, freightOrderId,
                "INC-101", "Damaged turbine crates during transport",
                new BigDecimal("20000.00"), null, null, null, null,
                ClaimStatus.OPEN, null, List.of(),
                now, now, "manager", "manager", 0L
        );

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.OPEN);

        // Cannot approve unassessed claim
        assertThatThrownBy(() -> claim.approve("manager", now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must have an assessed amount greater than zero");

        // Assess claim
        FreightInsuranceClaim assessed = claim.assess(new BigDecimal("18000.00"), "Inspected at depot; approved loss 18,000", "adjuster", now);
        assertThat(assessed.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(assessed.getAssessedAmount()).isEqualByComparingTo("18000.00");
        assertThat(assessed.getAssessedBy()).isEqualTo("adjuster");

        // Approve claim
        FreightInsuranceClaim approved = assessed.approve("manager", now);
        assertThat(approved.getStatus()).isEqualTo(ClaimStatus.APPROVED);

        // Partial settlement 1: 10,000
        FreightInsuranceClaim settled1 = approved.recordSettlement(
                UUID.randomUUID(), "SETTLE-001", new BigDecimal("10000.00"), "USD", "Initial tranche", "finance", now
        );
        assertThat(settled1.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(settled1.getTotalSettledAmount()).isEqualByComparingTo("10000.00");
        assertThat(settled1.getRemainingApprovedAmount()).isEqualByComparingTo("8000.00");
        assertThat(settled1.getSettlements()).hasSize(1);

        // Attempt over-settlement: 9,000 (10,000 + 9,000 = 19,000 > 18,000)
        assertThatThrownBy(() -> settled1.recordSettlement(
                UUID.randomUUID(), "SETTLE-002", new BigDecimal("9000.00"), "USD", "Over-settlement", "finance", now
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds approved assessed amount");

        // Final settlement tranche: 8,000 (total = 18,000 -> status transitions to SETTLED)
        FreightInsuranceClaim settled2 = settled1.recordSettlement(
                UUID.randomUUID(), "SETTLE-002", new BigDecimal("8000.00"), "USD", "Final tranche", "finance", now
        );
        assertThat(settled2.getStatus()).isEqualTo(ClaimStatus.SETTLED);
        assertThat(settled2.getTotalSettledAmount()).isEqualByComparingTo("18000.00");
        assertThat(settled2.getRemainingApprovedAmount()).isEqualByComparingTo("0.00");
        assertThat(settled2.getSettlements()).hasSize(2);
    }

    @Test
    @DisplayName("Claim lifecycle: reject and dispute")
    void testClaimRejectAndDispute() {
        FreightInsuranceClaim claim = new FreightInsuranceClaim(
                claimId, "CLM-2026-000002", policyId, freightOrderId,
                "INC-102", "Minor carton tear",
                new BigDecimal("500.00"), null, null, null, null,
                ClaimStatus.OPEN, null, List.of(),
                now, now, "manager", "manager", 0L
        );

        // Reject claim
        FreightInsuranceClaim rejected = claim.reject("Packaging improper by shipper", "adjuster", now);
        assertThat(rejected.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(rejected.getResolutionReason()).isEqualTo("Packaging improper by shipper");

        // Dispute rejected claim
        FreightInsuranceClaim disputed = rejected.dispute("Shipper provided certified packaging certificate", "shipper", now);
        assertThat(disputed.getStatus()).isEqualTo(ClaimStatus.DISPUTED);
        assertThat(disputed.getResolutionReason()).isEqualTo("Shipper provided certified packaging certificate");

        // Cannot settle a disputed/rejected claim
        assertThatThrownBy(() -> disputed.recordSettlement(
                UUID.randomUUID(), "SETTLE-ERR", new BigDecimal("500.00"), "USD", null, "finance", now
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot settle a claim that is not approved");
    }
}
