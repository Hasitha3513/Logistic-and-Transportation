package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ClaimSettlementResponse(
        UUID id,
        UUID claimId,
        String settlementReference,
        BigDecimal amount,
        String currency,
        String notes,
        String settledBy,
        OffsetDateTime settledAt
) {}
