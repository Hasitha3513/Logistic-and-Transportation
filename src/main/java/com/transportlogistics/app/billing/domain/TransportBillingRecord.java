package com.transportlogistics.app.billing.domain;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record TransportBillingRecord(UUID id, UUID tenantId, String billingNumber, RecordType recordType,
    UUID originalBillingRecordId, UUID replacementOfRecordId, Source source, UUID customerId, String currency,
    Lifecycle lifecycle, List<Line> lines, TaxFact tax, List<CostCentre> costCentres, Totals totals,
    UUID preparedBy, UUID approvedBy, OffsetDateTime approvedAt, OffsetDateTime finalizedAt,
    UUID exportConfigurationId, UUID exportEventId, String validationHash, Compliance compliance,
    long version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public TransportBillingRecord {
        if (id == null || tenantId == null || recordType == null || source == null || customerId == null
            || preparedBy == null || createdAt == null || updatedAt == null || version < 0) invalid("Invalid billing record");
        currency = currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
        if (currency == null || !currency.matches("[A-Z]{3}")) invalid("Currency must be ISO-4217");
        lines = List.copyOf(lines == null ? List.of() : lines);
        costCentres = List.copyOf(costCentres == null ? List.of() : costCentres);
        tax = tax == null ? TaxFact.notSupplied() : tax;
        totals = totals == null ? Totals.calculate(lines, tax, recordType) : totals;
        compliance = compliance == null ? Compliance.PENDING : compliance;
        if (recordType == RecordType.REGULAR && originalBillingRecordId != null) invalid("Regular record cannot reference reversal original");
        if (recordType == RecordType.REVERSAL && originalBillingRecordId == null) invalid("Reversal original is required");
    }

    public TransportBillingRecord replace(List<Line> newLines, TaxFact newTax, List<CostCentre> newCentres,
                                           OffsetDateTime now) {
        if (lifecycle != Lifecycle.DRAFT && lifecycle != Lifecycle.VALIDATED) state();
        return copy(Lifecycle.DRAFT, newLines, newTax, newCentres, Totals.calculate(newLines, newTax, recordType),
            null, null, null, null, null, null, Compliance.PENDING, now);
    }

    public TransportBillingRecord validated(String hash, UUID configurationId, Compliance decision, OffsetDateTime now) {
        if (lifecycle != Lifecycle.DRAFT && lifecycle != Lifecycle.VALIDATED) state();
        validateCommercial();
        return copy(Lifecycle.VALIDATED, lines, tax, costCentres, Totals.calculate(lines, tax, recordType),
            null, null, null, configurationId, null, hash, decision, now);
    }

    public TransportBillingRecord approved(UUID actor, OffsetDateTime now) {
        if (lifecycle != Lifecycle.VALIDATED) state();
        if (preparedBy.equals(actor)) throw rule("BILLING_SOD_VIOLATION");
        return copy(Lifecycle.APPROVED, lines, tax, costCentres, totals, actor, now, null,
            exportConfigurationId, null, validationHash, compliance, now);
    }

    public TransportBillingRecord finalized(OffsetDateTime now) {
        if (lifecycle != Lifecycle.APPROVED) throw rule("BILLING_APPROVAL_REQUIRED");
        if (compliance == Compliance.PENDING) throw rule("BILLING_CALCULATION_INVALID");
        return copy(Lifecycle.FINALIZED, lines, tax, costCentres, totals, approvedBy, approvedAt, now,
            exportConfigurationId, null, validationHash, compliance, now);
    }

    public TransportBillingRecord cancelled(OffsetDateTime now) {
        if (lifecycle != Lifecycle.DRAFT && lifecycle != Lifecycle.VALIDATED) state();
        return copy(Lifecycle.CANCELLED, lines, tax, costCentres, totals, approvedBy, approvedAt, null,
            exportConfigurationId, null, validationHash, compliance, now);
    }

    public TransportBillingRecord exportRequested(UUID eventId, OffsetDateTime now) {
        if (lifecycle == Lifecycle.EXPORT_REQUESTED && eventId.equals(exportEventId)) return this;
        if (lifecycle != Lifecycle.FINALIZED) state();
        return copy(Lifecycle.EXPORT_REQUESTED, lines, tax, costCentres, totals, approvedBy, approvedAt, finalizedAt,
            exportConfigurationId, eventId, validationHash, compliance, now);
    }

    public TransportBillingRecord exported(OffsetDateTime now) {
        if (lifecycle == Lifecycle.EXPORTED) return this;
        if (lifecycle != Lifecycle.EXPORT_REQUESTED) state();
        return copy(Lifecycle.EXPORTED, lines, tax, costCentres, totals, approvedBy, approvedAt, finalizedAt,
            exportConfigurationId, exportEventId, validationHash, compliance, now);
    }

    public TransportBillingRecord reversed(OffsetDateTime now) {
        if (lifecycle == Lifecycle.REVERSED) return this;
        if (recordType != RecordType.REGULAR || (lifecycle != Lifecycle.FINALIZED && lifecycle != Lifecycle.EXPORT_REQUESTED
            && lifecycle != Lifecycle.EXPORTED)) state();
        return copy(Lifecycle.REVERSED, lines, tax, costCentres, totals, approvedBy, approvedAt, finalizedAt,
            exportConfigurationId, exportEventId, validationHash, compliance, now);
    }

    public void validateCommercial() {
        long bases = lines.stream().filter(l -> l.category == LineCategory.BASE_CHARGE).count();
        if (bases != 1 || lines.stream().anyMatch(l -> l.amount.signum() < 0)) throw rule("BILLING_CALCULATION_INVALID");
        BigDecimal allocation = costCentres.stream().map(CostCentre::allocationPercent)
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        if (costCentres.isEmpty() || allocation.compareTo(new BigDecimal("100.0000")) != 0)
            throw rule("BILLING_COST_CENTRE_INVALID");
        tax.validate(recordType == RecordType.REVERSAL ? totals.subtotal().abs() : totals.subtotal());
        if (recordType == RecordType.REGULAR && totals.totalAmount().signum() < 0)
            throw rule("BILLING_CALCULATION_INVALID");
    }

    private TransportBillingRecord copy(Lifecycle next, List<Line> nextLines, TaxFact nextTax,
        List<CostCentre> nextCentres, Totals nextTotals, UUID approver, OffsetDateTime approvalTime,
        OffsetDateTime finalTime, UUID configuration, UUID event, String hash, Compliance decision, OffsetDateTime now) {
        return new TransportBillingRecord(id, tenantId, billingNumber, recordType, originalBillingRecordId,
            replacementOfRecordId, source, customerId, currency, next, nextLines, nextTax, nextCentres, nextTotals,
            preparedBy, approver, approvalTime, finalTime, configuration, event, hash, decision, version, createdAt, now);
    }

    public enum RecordType { REGULAR, REVERSAL }
    public enum Lifecycle { DRAFT, VALIDATED, APPROVED, FINALIZED, EXPORT_REQUESTED, EXPORTED, CANCELLED, REVERSED }
    public enum LineCategory { BASE_CHARGE, SURCHARGE, PENALTY, CREDIT_ADJUSTMENT }
    public enum TaxStatus { SUPPLIED, NOT_SUPPLIED }
    public enum Compliance { ACCEPTED, NOT_REQUIRED, PENDING }
    public record Source(SourceType type, UUID id, String businessNumber, String terminalLifecycle,
                         OffsetDateTime completionTime, long sourceVersion, String snapshotHash) {
        public enum SourceType { TRIP, FREIGHT_ORDER }
    }
    public record Line(UUID id, LineCategory category, String reasonCode, String provenance,
                       BigDecimal quantity, BigDecimal unitRate, BigDecimal amount) {
        public Line {
            if (id == null || category == null || blank(reasonCode) || blank(provenance)
                || quantity == null || unitRate == null || amount == null) invalid("Invalid billing line");
            quantity = quantity.setScale(4, RoundingMode.HALF_UP);
            unitRate = unitRate.setScale(4, RoundingMode.HALF_UP);
            BigDecimal calculated = quantity.multiply(unitRate, new java.math.MathContext(19, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
            if (calculated.compareTo(amount.setScale(2, RoundingMode.HALF_UP)) != 0) invalid("Line amount mismatch");
            amount = calculated;
        }
    }
    public record TaxFact(TaxStatus status, String category, String jurisdictionReference, BigDecimal taxableAmount,
                          BigDecimal rate, BigDecimal taxAmount, String exemptionReference, String provenance,
                          String snapshotHash) {
        public TaxFact { taxAmount = money(taxAmount == null ? BigDecimal.ZERO : taxAmount); }
        public static TaxFact notSupplied() { return new TaxFact(TaxStatus.NOT_SUPPLIED, null, null, null, null,
            BigDecimal.ZERO, null, null, null); }
        void validate(BigDecimal subtotal) {
            if (status == TaxStatus.NOT_SUPPLIED) {
                if (taxAmount.signum() != 0 || category != null || jurisdictionReference != null || taxableAmount != null
                    || rate != null || exemptionReference != null) throw rule("BILLING_TAX_FACT_INVALID");
                return;
            }
            if (taxableAmount == null || rate == null || blank(provenance) || blank(snapshotHash))
                throw rule("BILLING_TAX_FACT_INVALID");
            BigDecimal expected = taxableAmount.multiply(rate, new java.math.MathContext(19, RoundingMode.HALF_UP))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (expected.compareTo(taxAmount) != 0 || taxableAmount.compareTo(subtotal) > 0)
                throw rule("BILLING_TAX_FACT_INVALID");
        }
    }
    public record CostCentre(UUID id, String code, BigDecimal allocationPercent, String description, String source) {
        public CostCentre { if (id == null || blank(code) || allocationPercent == null || blank(source))
            invalid("Invalid cost centre"); allocationPercent = allocationPercent.setScale(4, RoundingMode.HALF_UP); }
    }
    public record Totals(BigDecimal baseCharge, BigDecimal surcharges, BigDecimal penalties,
        BigDecimal creditAdjustments, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal totalAmount) {
        public static Totals calculate(List<Line> lines, TaxFact tax, RecordType type) {
            BigDecimal base=sum(lines,LineCategory.BASE_CHARGE), surcharge=sum(lines,LineCategory.SURCHARGE),
                penalty=sum(lines,LineCategory.PENALTY), credit=sum(lines,LineCategory.CREDIT_ADJUSTMENT);
            BigDecimal subtotal=money(base.add(surcharge).add(penalty).subtract(credit));
            BigDecimal taxAmount=money(tax == null ? BigDecimal.ZERO : tax.taxAmount());
            BigDecimal total=money(subtotal.add(taxAmount));
            if(type==RecordType.REVERSAL) return new Totals(base.negate(),surcharge.negate(),penalty.negate(),
                credit.negate(),subtotal.negate(),taxAmount.negate(),total.negate());
            return new Totals(base,surcharge,penalty,credit,subtotal,taxAmount,total);
        }
        private static BigDecimal sum(List<Line> lines,LineCategory c){return money(lines.stream().filter(l->l.category==c)
            .map(Line::amount).reduce(BigDecimal.ZERO,BigDecimal::add));}
    }
    private static BigDecimal money(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP);}
    private static boolean blank(String v){return v==null||v.isBlank();}
    private static void invalid(String m){throw new IllegalArgumentException(m);}
    private static void state(){throw rule("BILLING_INVALID_STATE");}
    private static BusinessRuleException rule(String c){return new BusinessRuleException(c,c);}
}
