package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FreightInsuranceClaimResponse(
        UUID id,
        String claimNumber,
        UUID policyId,
        UUID freightOrderId,
        String incidentReference,
        String damageDescription,
        BigDecimal claimedAmount,
        BigDecimal assessedAmount,
        String assessmentNotes,
        String assessedBy,
        OffsetDateTime assessedAt,
        String status,
        String resolutionReason,
        BigDecimal totalSettledAmount,
        BigDecimal remainingApprovedAmount,
        List<ClaimSettlementResponse> settlements,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
