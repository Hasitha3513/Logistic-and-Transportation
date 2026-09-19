package com.transportlogistics.app.compliance.ports.outbound;

import com.transportlogistics.app.compliance.domain.ComplianceSourceFactReference;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BillingTaxFactsQuery {

    Optional<BillingTaxFact> findAt(UUID tenantId, UUID billingRecordId, Instant effectiveAt);

    record BillingTaxFact(
            ComplianceSourceFactReference reference,
            String suppliedState,
            String category,
            String jurisdiction,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            String exemptionCode,
            String provenanceCode,
            String snapshotHash) {
    }
}
