package com.transportlogistics.app.fleet.payroll.domain;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DriverPayrollInputBatch(UUID id, UUID tenantId, Type type, UUID correctionOfBatchId,
                                      LocalDate periodStart, LocalDate periodEndExclusive, OffsetDateTime cutoffAt,
                                      String currency, Lifecycle lifecycle, List<DriverPayrollInputLine> lines,
                                      Totals totals, UUID preparedBy, UUID approvedBy, OffsetDateTime approvedAt,
                                      UUID exportConfigurationId, UUID exportEventId, String validationHash,
                                      long version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public DriverPayrollInputBatch {
        lines = List.copyOf(lines == null ? List.of() : lines);
        if (id == null || tenantId == null || type == null || periodStart == null || periodEndExclusive == null
                || !periodStart.isBefore(periodEndExclusive) || cutoffAt == null || preparedBy == null
                || createdAt == null || updatedAt == null) invalid("Invalid batch");
        currency = currency == null ? null : currency.trim().toUpperCase(java.util.Locale.ROOT);
        if (currency == null || !currency.matches("[A-Z]{3}")) invalid("Currency must be ISO-4217");
        if (type == Type.CORRECTION && correctionOfBatchId == null) invalid("Correction source is required");
        if (type == Type.REGULAR && correctionOfBatchId != null) invalid("Regular batch cannot have correction source");
        totals = totals == null ? Totals.zero() : totals;
    }

    public DriverPayrollInputBatch replaceLines(List<DriverPayrollInputLine> replacements, OffsetDateTime now) {
        if (lifecycle != Lifecycle.DRAFT && lifecycle != Lifecycle.VALIDATED) invalidState();
        return copy(Lifecycle.DRAFT, replacements, Totals.calculate(replacements), null, null, null, null,
            null, null, version, now);
    }

    public DriverPayrollInputBatch validated(List<DriverPayrollInputLine> snapshots, String hash,
                                              UUID configurationId, OffsetDateTime now) {
        if (lifecycle != Lifecycle.DRAFT && lifecycle != Lifecycle.VALIDATED) invalidState();
        if (snapshots.isEmpty()) invalid("At least one line is required");
        return copy(Lifecycle.VALIDATED, snapshots, Totals.calculate(snapshots), null, null, configurationId,
            null, hash, null, version, now);
    }

    public DriverPayrollInputBatch approved(UUID actor, OffsetDateTime now) {
        if (lifecycle != Lifecycle.VALIDATED) invalidState();
        if (preparedBy.equals(actor)) throw rule("DRIVER_PAYROLL_SOD_VIOLATION");
        return copy(Lifecycle.APPROVED, lines, totals, actor, now, exportConfigurationId, null,
            validationHash, null, version, now);
    }

    public DriverPayrollInputBatch exportRequested(UUID eventId, OffsetDateTime now) {
        if (lifecycle == Lifecycle.EXPORT_REQUESTED && eventId.equals(exportEventId)) return this;
        if (lifecycle != Lifecycle.APPROVED) throw rule("DRIVER_PAYROLL_ALREADY_RELEASED");
        return copy(Lifecycle.EXPORT_REQUESTED, lines, totals, approvedBy, approvedAt, exportConfigurationId,
            eventId, validationHash, null, version, now);
    }

    private DriverPayrollInputBatch copy(Lifecycle state, List<DriverPayrollInputLine> nextLines, Totals nextTotals,
                                         UUID approver, OffsetDateTime approvalTime, UUID configurationId,
                                         UUID eventId, String hash, UUID unused, long nextVersion, OffsetDateTime now) {
        return new DriverPayrollInputBatch(id, tenantId, type, correctionOfBatchId, periodStart,
            periodEndExclusive, cutoffAt, currency, state, nextLines, nextTotals, preparedBy, approver,
            approvalTime, configurationId, eventId, hash, nextVersion, createdAt, now);
    }

    private static void invalidState() { throw rule("DRIVER_PAYROLL_INVALID_STATE"); }
    private static void invalid(String message) { throw new IllegalArgumentException(message); }
    private static BusinessRuleException rule(String code) { return new BusinessRuleException(code, code); }
    public enum Type { REGULAR, CORRECTION }
    public enum Lifecycle { DRAFT, VALIDATED, APPROVED, EXPORT_REQUESTED, EXPORTED, SUPERSEDED }

    public record Totals(BigDecimal tripEarnings, BigDecimal allowances, BigDecimal overtime,
                         BigDecimal deductions, BigDecimal provisionalNetInput) {
        static Totals zero() { return new Totals(money(BigDecimal.ZERO), money(BigDecimal.ZERO),
            money(BigDecimal.ZERO), money(BigDecimal.ZERO), money(BigDecimal.ZERO)); }
        public static Totals calculate(List<DriverPayrollInputLine> lines) {
            BigDecimal trip = BigDecimal.ZERO, allowance = BigDecimal.ZERO, overtime = BigDecimal.ZERO,
                deduction = BigDecimal.ZERO;
            for (var line : lines) switch (line.category()) {
                case TRIP_EARNING -> trip = trip.add(line.amount());
                case ALLOWANCE -> allowance = allowance.add(line.amount());
                case OVERTIME -> overtime = overtime.add(line.amount());
                case DEDUCTION -> deduction = deduction.add(line.amount());
            }
            return new Totals(money(trip), money(allowance), money(overtime), money(deduction),
                money(trip.add(allowance).add(overtime).subtract(deduction)));
        }
        private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    }
}
