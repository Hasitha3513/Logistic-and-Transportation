package com.transportlogistics.app.freight.insurance.domain;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a Freight Insurance Policy association.
 */
public final class FreightInsurancePolicy {

    private final UUID id;
    private final String policyNumber;
    private final UUID freightOrderId;
    private final UUID cargoManifestId;
    private final String insuranceProvider;
    private final String policyType;
    private final BigDecimal coverageAmount;
    private final BigDecimal premiumAmount;
    private final String currency;
    private final OffsetDateTime validFrom;
    private final OffsetDateTime validUntil;
    private final PolicyStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final String createdBy;
    private final String updatedBy;
    private final long version;

    public FreightInsurancePolicy(UUID id,
                                  String policyNumber,
                                  UUID freightOrderId,
                                  UUID cargoManifestId,
                                  String insuranceProvider,
                                  String policyType,
                                  BigDecimal coverageAmount,
                                  BigDecimal premiumAmount,
                                  String currency,
                                  OffsetDateTime validFrom,
                                  OffsetDateTime validUntil,
                                  PolicyStatus status,
                                  OffsetDateTime createdAt,
                                  OffsetDateTime updatedAt,
                                  String createdBy,
                                  String updatedBy,
                                  long version) {
        this.id = Objects.requireNonNull(id, "Policy ID is required");
        if (policyNumber == null || policyNumber.isBlank()) {
            throw new IllegalArgumentException("Policy number is required");
        }
        this.policyNumber = policyNumber;
        this.freightOrderId = Objects.requireNonNull(freightOrderId, "Freight order ID is required");
        this.cargoManifestId = cargoManifestId;
        if (insuranceProvider == null || insuranceProvider.isBlank()) {
            throw new IllegalArgumentException("Insurance provider is required");
        }
        this.insuranceProvider = insuranceProvider;
        if (policyType == null || policyType.isBlank()) {
            throw new IllegalArgumentException("Policy type is required");
        }
        this.policyType = policyType;
        if (coverageAmount == null || coverageAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Coverage amount must be greater than zero");
        }
        this.coverageAmount = coverageAmount;
        if (premiumAmount == null || premiumAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Premium amount must be non-negative");
        }
        this.premiumAmount = premiumAmount;
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        this.currency = currency;
        this.validFrom = Objects.requireNonNull(validFrom, "Valid from date is required");
        this.validUntil = Objects.requireNonNull(validUntil, "Valid until date is required");
        if (validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException("Valid until date cannot be before valid from date");
        }
        this.status = status != null ? status : PolicyStatus.ACTIVE;
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
    public String getPolicyNumber() { return policyNumber; }
    public UUID getFreightOrderId() { return freightOrderId; }
    public UUID getCargoManifestId() { return cargoManifestId; }
    public String getInsuranceProvider() { return insuranceProvider; }
    public String getPolicyType() { return policyType; }
    public BigDecimal getCoverageAmount() { return coverageAmount; }
    public BigDecimal getPremiumAmount() { return premiumAmount; }
    public String getCurrency() { return currency; }
    public OffsetDateTime getValidFrom() { return validFrom; }
    public OffsetDateTime getValidUntil() { return validUntil; }
    public PolicyStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public long getVersion() { return version; }

    public boolean isValidAt(OffsetDateTime time) {
        if (status != PolicyStatus.ACTIVE) {
            return false;
        }
        return !time.isBefore(validFrom) && !time.isAfter(validUntil);
    }

    public void validateCoverageFor(BigDecimal claimedAmount, OffsetDateTime incidentTime) {
        if (!isValidAt(incidentTime)) {
            throw new BusinessRuleException("INSURANCE_POLICY_EXPIRED", "Policy is not active or valid at incident time: " + incidentTime);
        }
        if (claimedAmount != null && claimedAmount.compareTo(coverageAmount) > 0) {
            throw new BusinessRuleException("INSURANCE_COVERAGE_INSUFFICIENT",
                    "Claimed amount (" + claimedAmount + ") exceeds policy coverage limit (" + coverageAmount + ")");
        }
    }

    public FreightInsurancePolicy update(String insuranceProvider,
                                         String policyType,
                                         BigDecimal coverageAmount,
                                         BigDecimal premiumAmount,
                                         OffsetDateTime validFrom,
                                         OffsetDateTime validUntil,
                                         PolicyStatus status,
                                         String actor,
                                         OffsetDateTime now) {
        return new FreightInsurancePolicy(
                this.id,
                this.policyNumber,
                this.freightOrderId,
                this.cargoManifestId,
                insuranceProvider != null ? insuranceProvider : this.insuranceProvider,
                policyType != null ? policyType : this.policyType,
                coverageAmount != null ? coverageAmount : this.coverageAmount,
                premiumAmount != null ? premiumAmount : this.premiumAmount,
                this.currency,
                validFrom != null ? validFrom : this.validFrom,
                validUntil != null ? validUntil : this.validUntil,
                status != null ? status : this.status,
                this.createdAt,
                now,
                this.createdBy,
                actor,
                this.version
        );
    }
}
