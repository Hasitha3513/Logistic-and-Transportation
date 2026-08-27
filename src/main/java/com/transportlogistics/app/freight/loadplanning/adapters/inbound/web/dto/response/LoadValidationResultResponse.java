package com.transportlogistics.app.freight.loadplanning.adapters.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LoadValidationResultResponse(
        UUID loadPlanId,
        OffsetDateTime validatedAt,
        String validatedBy,
        String overallOutcome,
        BigDecimal cargoWeightKg,
        BigDecimal payloadCapacityKg,
        BigDecimal payloadUtilizationPercent,
        BigDecimal cargoVolumeM3,
        BigDecimal volumeCapacityM3,
        BigDecimal volumeUtilizationPercent,
        BigDecimal projectedGrossWeightKg,
        BigDecimal grossWeightLimitKg,
        BigDecimal tareWeightKg,
        BigDecimal grossWeightKg,
        BigDecimal netWeightKg,
        BigDecimal cubicVolumeM3,
        String payloadResult,
        String volumeResult,
        String gvwResult,
        String axleResult,
        List<ViolationDetail> violations,
        List<String> missingData
) {
    public record ViolationDetail(
            String code,
            String message
    ) {}
}
