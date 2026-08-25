package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "freight_insurance_claim")
public class FreightInsuranceClaimEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 60)
    private String claimNumber;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "freight_order_id", nullable = false)
    private UUID freightOrderId;

    @Column(name = "incident_reference", length = 120)
    private String incidentReference;

    @Column(name = "damage_description", nullable = false, length = 2000)
    private String damageDescription;

    @Column(name = "claimed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal claimedAmount;

    @Column(name = "assessed_amount", precision = 19, scale = 4)
    private BigDecimal assessedAmount;

    @Column(name = "assessment_notes", length = 2000)
    private String assessmentNotes;

    @Column(name = "assessed_by", length = 128)
    private String assessedBy;

    @Column(name = "assessed_at")
    private OffsetDateTime assessedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "resolution_reason", length = 2000)
    private String resolutionReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("settledAt ASC")
    private List<FreightInsuranceSettlementEntity> settlements = new ArrayList<>();

    public FreightInsuranceClaimEntity() {}

    public void replaceSettlements(List<FreightInsuranceSettlementEntity> newSettlements) {
        settlements.clear();
        if (newSettlements != null) {
            newSettlements.forEach(s -> {
                s.setClaim(this);
                settlements.add(s);
            });
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }

    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }

    public UUID getFreightOrderId() { return freightOrderId; }
    public void setFreightOrderId(UUID freightOrderId) { this.freightOrderId = freightOrderId; }

    public String getIncidentReference() { return incidentReference; }
    public void setIncidentReference(String incidentReference) { this.incidentReference = incidentReference; }

    public String getDamageDescription() { return damageDescription; }
    public void setDamageDescription(String damageDescription) { this.damageDescription = damageDescription; }

    public BigDecimal getClaimedAmount() { return claimedAmount; }
    public void setClaimedAmount(BigDecimal claimedAmount) { this.claimedAmount = claimedAmount; }

    public BigDecimal getAssessedAmount() { return assessedAmount; }
    public void setAssessedAmount(BigDecimal assessedAmount) { this.assessedAmount = assessedAmount; }

    public String getAssessmentNotes() { return assessmentNotes; }
    public void setAssessmentNotes(String assessmentNotes) { this.assessmentNotes = assessmentNotes; }

    public String getAssessedBy() { return assessedBy; }
    public void setAssessedBy(String assessedBy) { this.assessedBy = assessedBy; }

    public OffsetDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(OffsetDateTime assessedAt) { this.assessedAt = assessedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolutionReason() { return resolutionReason; }
    public void setResolutionReason(String resolutionReason) { this.resolutionReason = resolutionReason; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public List<FreightInsuranceSettlementEntity> getSettlements() { return settlements; }
    public void setSettlements(List<FreightInsuranceSettlementEntity> settlements) { this.settlements = settlements; }
}
