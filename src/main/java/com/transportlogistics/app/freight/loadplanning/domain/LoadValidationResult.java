package com.transportlogistics.app.freight.loadplanning.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Result of US-27 weight, volume, payload, and vehicle capacity validation.
 */
public record LoadValidationResult(
        UUID loadPlanId,
        OffsetDateTime validatedAt,
        String validatedBy,
        ValidationOutcome overallOutcome,
        BigDecimal cargoWeightKg,
        BigDecimal payloadCapacityKg,
        BigDecimal payloadUtilizationPercent,
        BigDecimal cargoVolumeM3,
        BigDecimal volumeCapacityM3,
        BigDecimal volumeUtilizationPercent,
        BigDecimal projectedGrossWeightKg,
        BigDecimal grossWeightLimitKg,
        BigDecimal tareWeightKg,
        ValidationOutcome payloadResult,
        ValidationOutcome volumeResult,
        ValidationOutcome gvwResult,
        ValidationOutcome axleResult,
        List<LoadValidationViolation> violations,
        List<String> missingData
) {
    public LoadValidationResult(
            UUID loadPlanId,
            OffsetDateTime validatedAt,
            String validatedBy,
            ValidationOutcome overallOutcome,
            BigDecimal grossWeightKg,
            BigDecimal netWeightKg,
            BigDecimal cubicVolumeM3,
            ValidationOutcome payloadResult,
            ValidationOutcome volumeResult,
            ValidationOutcome axleResult,
            List<LoadValidationViolation> violations,
            List<String> missingData
    ) {
        this(loadPlanId, validatedAt, validatedBy, overallOutcome,
                netWeightKg, null, null,
                cubicVolumeM3, null, null,
                grossWeightKg, null, null,
                payloadResult, volumeResult, ValidationOutcome.INCOMPLETE, axleResult,
                violations, missingData);
    }

    public BigDecimal grossWeightKg() {
        return projectedGrossWeightKg;
    }

    public BigDecimal netWeightKg() {
        return cargoWeightKg;
    }

    public BigDecimal cubicVolumeM3() {
        return cargoVolumeM3;
    }
}
