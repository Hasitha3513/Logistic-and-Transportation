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
        BigDecimal grossWeightKg,
        BigDecimal netWeightKg,
        BigDecimal cubicVolumeM3,
        String payloadResult,
        String volumeResult,
        String axleResult,
        List<ViolationDetail> violations,
        List<String> missingData
) {
    public record ViolationDetail(
            String code,
            String message
    ) {}
}
