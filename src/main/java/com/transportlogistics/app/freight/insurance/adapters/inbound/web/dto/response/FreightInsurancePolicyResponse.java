package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FreightInsurancePolicyResponse(
        UUID id,
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
        String status,
        boolean active,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
