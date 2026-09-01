package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SingleOrderEtaEstimate(
        UUID orderId,
        OffsetDateTime estimatedArrivalAt,
        long travelDurationSeconds,
        long distanceMeters,
        EtaStatus slaStatus,
        EtaSource source,
        OffsetDateTime calculatedAt,
        OffsetDateTime staleAt
) {
    public SingleOrderEtaEstimate {
        if (orderId == null) throw new BusinessRuleException("ORDER_ID_REQUIRED", "Order ID is required");
        if (estimatedArrivalAt == null) throw new BusinessRuleException("ESTIMATED_ARRIVAL_REQUIRED", "Estimated arrival time is required");
        if (travelDurationSeconds < 0) throw new BusinessRuleException("INVALID_DURATION", "Travel duration cannot be negative");
        if (distanceMeters < 0) throw new BusinessRuleException("INVALID_DISTANCE", "Distance cannot be negative");
        if (source == null) throw new BusinessRuleException("SOURCE_REQUIRED", "ETA source is required");
        if (calculatedAt == null) throw new BusinessRuleException("CALCULATED_AT_REQUIRED", "Calculated at timestamp is required");
        if (staleAt == null) throw new BusinessRuleException("STALE_AT_REQUIRED", "Stale at timestamp is required");
    }

    public boolean isStale(OffsetDateTime now) {
        return now != null && now.isAfter(staleAt);
    }
}
