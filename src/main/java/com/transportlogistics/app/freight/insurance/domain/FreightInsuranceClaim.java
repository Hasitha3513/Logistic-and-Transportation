package com.transportlogistics.app.freight.insurance.domain;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing an Insurance Damage Claim with workflow transitions
 * and immutable settlement history.
 */
public final class FreightInsuranceClaim {

    private final UUID id;
    private final String claimNumber;
    private final UUID policyId;
    private final UUID freightOrderId;
    private final String incidentReference;
    private final String damageDescription;
    private final BigDecimal claimedAmount;
    private final BigDecimal assessedAmount;
    private final String assessmentNotes;
    private final String assessedBy;
    private final OffsetDateTime assessedAt;
    private final ClaimStatus status;
    private final String resolutionReason;
    private final List<ClaimSettlement> settlements;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final String createdBy;
    private final String updatedBy;
    private final long version;

    public FreightInsuranceClaim(UUID id,
                                 String claimNumber,
                                 UUID policyId,
                                 UUID freightOrderId,
                                 String incidentReference,
                                 String damageDescription,
                                 BigDecimal claimedAmount,
                                 BigDecimal assessedAmount,
                                 String assessmentNotes,
                                 String assessedBy,
                                 OffsetDateTime assessedAt,
                                 ClaimStatus status,
                                 String resolutionReason,
                                 List<ClaimSettlement> settlements,
                                 OffsetDateTime createdAt,
                                 OffsetDateTime updatedAt,
                                 String createdBy,
                                 String updatedBy,
                                 long version) {
        this.id = Objects.requireNonNull(id, "Claim ID is required");
        if (claimNumber == null || claimNumber.isBlank()) {
            throw new IllegalArgumentException("Claim number is required");
        }
        this.claimNumber = claimNumber;
        this.policyId = Objects.requireNonNull(policyId, "Policy ID is required");
        this.freightOrderId = Objects.requireNonNull(freightOrderId, "Freight order ID is required");
        this.incidentReference = incidentReference;
        if (damageDescription == null || damageDescription.isBlank()) {
            throw new IllegalArgumentException("Damage description is required");
        }
        this.damageDescription = damageDescription;
        if (claimedAmount == null || claimedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Claimed amount must be greater than zero");
        }
        this.claimedAmount = claimedAmount;
        this.assessedAmount = assessedAmount;
        this.assessmentNotes = assessmentNotes;
        this.assessedBy = assessedBy;
        this.assessedAt = assessedAt;
        this.status = status != null ? status : ClaimStatus.OPEN;
        this.resolutionReason = resolutionReason;
        this.settlements = settlements == null ? List.of() : List.copyOf(settlements);
        this.createdAt = Objects.requireNonNull(createdAt, "Created at timestamp is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at timestamp is required");
        this.createdBy = Objects.requireNonNull(createdBy, "Created by actor is required");
        this.updatedBy = Objects.requireNonNull(updatedBy, "Updated by actor is required");
        if (version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
        this.version = version;
    }

    public UUID getId() { return id; }
    public String getClaimNumber() { return claimNumber; }
    public UUID getPolicyId() { return policyId; }
    public UUID getFreightOrderId() { return freightOrderId; }
    public String getIncidentReference() { return incidentReference; }
    public String getDamageDescription() { return damageDescription; }
    public BigDecimal getClaimedAmount() { return claimedAmount; }
    public BigDecimal getAssessedAmount() { return assessedAmount; }
    public String getAssessmentNotes() { return assessmentNotes; }
    public String getAssessedBy() { return assessedBy; }
    public OffsetDateTime getAssessedAt() { return assessedAt; }
    public ClaimStatus getStatus() { return status; }
    public String getResolutionReason() { return resolutionReason; }
    public List<ClaimSettlement> getSettlements() { return settlements; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public long getVersion() { return version; }

    public BigDecimal getTotalSettledAmount() {
        return settlements.stream()
                .map(ClaimSettlement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingApprovedAmount() {
        if (assessedAmount == null) {
            return BigDecimal.ZERO;
        }
        return assessedAmount.subtract(getTotalSettledAmount()).max(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────────────────
    // Workflow Transitions
    // ──────────────────────────────────────────────────────────

    public FreightInsuranceClaim assess(BigDecimal assessedAmount,
                                        String assessmentNotes,
                                        String actor,
                                        OffsetDateTime now) {
        if (status == ClaimStatus.REJECTED || status == ClaimStatus.SETTLED) {
            throw new ConflictException("INSURANCE_CLAIM_INVALID_STATE", "Cannot assess a claim in status " + status);
        }
        if (assessedAmount == null || assessedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_ASSESSED_AMOUNT", "Assessed amount must be non-negative");
        }

        return new FreightInsuranceClaim(
                this.id,
                this.claimNumber,
                this.policyId,
                this.freightOrderId,
                this.incidentReference,
                this.damageDescription,
                this.claimedAmount,
                assessedAmount,
                assessmentNotes,
                actor,
                now,
                ClaimStatus.UNDER_REVIEW,
                this.resolutionReason,
                this.settlements,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }

    public FreightInsuranceClaim approve(String actor, OffsetDateTime now) {
        if (status != ClaimStatus.UNDER_REVIEW && status != ClaimStatus.OPEN) {
            throw new ConflictException("INSURANCE_CLAIM_INVALID_STATE", "Claim cannot be approved from status " + status);
        }
        if (assessedAmount == null || assessedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INSURANCE_CLAIM_UNASSESSED", "Claim must have an assessed amount greater than zero before approval");
        }

        return new FreightInsuranceClaim(
                this.id,
                this.claimNumber,
                this.policyId,
                this.freightOrderId,
                this.incidentReference,
                this.damageDescription,
                this.claimedAmount,
                this.assessedAmount,
                this.assessmentNotes,
                this.assessedBy,
                this.assessedAt,
                ClaimStatus.APPROVED,
                null,
                this.settlements,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }

    public FreightInsuranceClaim reject(String reason, String actor, OffsetDateTime now) {
        if (status == ClaimStatus.APPROVED || status == ClaimStatus.SETTLED) {
            throw new ConflictException("INSURANCE_CLAIM_INVALID_STATE", "Cannot reject an already approved or settled claim");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("REJECTION_REASON_REQUIRED", "Rejection reason is required");
        }

        return new FreightInsuranceClaim(
                this.id,
                this.claimNumber,
                this.policyId,
                this.freightOrderId,
                this.incidentReference,
                this.damageDescription,
                this.claimedAmount,
                this.assessedAmount,
                this.assessmentNotes,
                this.assessedBy,
                this.assessedAt,
                ClaimStatus.REJECTED,
                reason,
                this.settlements,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }

    public FreightInsuranceClaim dispute(String reason, String actor, OffsetDateTime now) {
        if (status != ClaimStatus.REJECTED && status != ClaimStatus.UNDER_REVIEW) {
            throw new ConflictException("INSURANCE_CLAIM_INVALID_STATE", "Only rejected or under-review claims can be disputed");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("DISPUTE_REASON_REQUIRED", "Dispute reason is required");
        }

        return new FreightInsuranceClaim(
                this.id,
                this.claimNumber,
                this.policyId,
                this.freightOrderId,
                this.incidentReference,
                this.damageDescription,
                this.claimedAmount,
                this.assessedAmount,
                this.assessmentNotes,
                this.assessedBy,
                this.assessedAt,
                ClaimStatus.DISPUTED,
                reason,
                this.settlements,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }

    public FreightInsuranceClaim recordSettlement(UUID settlementId,
                                                  String settlementRef,
                                                  BigDecimal amount,
                                                  String currency,
                                                  String notes,
                                                  String actor,
                                                  OffsetDateTime now) {
        if (status != ClaimStatus.APPROVED && status != ClaimStatus.UNDER_REVIEW) {
            throw new ConflictException("INSURANCE_CLAIM_NOT_APPROVED", "Cannot settle a claim that is not approved (status: " + status + ")");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_SETTLEMENT_AMOUNT", "Settlement amount must be greater than zero");
        }

        BigDecimal existingTotal = getTotalSettledAmount();
        BigDecimal newTotal = existingTotal.add(amount);

        if (assessedAmount != null && newTotal.compareTo(assessedAmount) > 0) {
            throw new BusinessRuleException("INSURANCE_SETTLEMENT_EXCEEDS_ALLOWED_AMOUNT",
                    "Total settlement (" + newTotal + ") exceeds approved assessed amount (" + assessedAmount + ")");
        }

        List<ClaimSettlement> newSettlements = new ArrayList<>(this.settlements);
        newSettlements.add(new ClaimSettlement(
                settlementId != null ? settlementId : UUID.randomUUID(),
                this.id,
                settlementRef,
                amount,
                currency,
                notes,
                actor,
                now
        ));

        ClaimStatus newStatus = (assessedAmount != null && newTotal.compareTo(assessedAmount) >= 0)
                ? ClaimStatus.SETTLED
                : this.status;

        return new FreightInsuranceClaim(
                this.id,
                this.claimNumber,
                this.policyId,
                this.freightOrderId,
                this.incidentReference,
                this.damageDescription,
                this.claimedAmount,
                this.assessedAmount,
                this.assessmentNotes,
                this.assessedBy,
                this.assessedAt,
                newStatus,
                this.resolutionReason,
                newSettlements,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }
}
