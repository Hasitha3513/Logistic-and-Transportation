package com.transportlogistics.app.freight.insurance.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "freight_insurance_settlement")
public class FreightInsuranceSettlementEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private FreightInsuranceClaimEntity claim;

    @Column(name = "settlement_reference", nullable = false, length = 120)
    private String settlementReference;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "settled_by", nullable = false, length = 128)
    private String settledBy;

    @Column(name = "settled_at", nullable = false)
    private OffsetDateTime settledAt;

    public FreightInsuranceSettlementEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public FreightInsuranceClaimEntity getClaim() { return claim; }
    public void setClaim(FreightInsuranceClaimEntity claim) { this.claim = claim; }

    public String getSettlementReference() { return settlementReference; }
    public void setSettlementReference(String settlementReference) { this.settlementReference = settlementReference; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSettledBy() { return settledBy; }
    public void setSettledBy(String settledBy) { this.settledBy = settledBy; }

    public OffsetDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(OffsetDateTime settledAt) { this.settledAt = settledAt; }
}
