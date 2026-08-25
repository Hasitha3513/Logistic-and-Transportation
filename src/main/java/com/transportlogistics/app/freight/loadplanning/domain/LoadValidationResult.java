package com.transportlogistics.app.freight.loadplanning.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Result of US-27 weight, volume, payload, and axle validation.
 */
public record LoadValidationResult(
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
}
