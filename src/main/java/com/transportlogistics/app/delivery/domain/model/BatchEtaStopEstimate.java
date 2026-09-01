package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BatchEtaStopEstimate(
        UUID deliveryOrderId,
        int sequence,
        OffsetDateTime estimatedArrivalAt,
        long travelDurationSeconds,
        long serviceDurationSeconds,
        long distanceMeters,
        EtaStatus slaStatus
) {
    public BatchEtaStopEstimate {
        if (deliveryOrderId == null) throw new BusinessRuleException("ORDER_ID_REQUIRED", "Delivery order ID is required");
        if (sequence < 0) throw new BusinessRuleException("INVALID_SEQUENCE", "Sequence cannot be negative");
        if (estimatedArrivalAt == null) throw new BusinessRuleException("ESTIMATED_ARRIVAL_REQUIRED", "Estimated arrival time is required");
        if (travelDurationSeconds < 0) throw new BusinessRuleException("INVALID_TRAVEL_DURATION", "Travel duration cannot be negative");
        if (serviceDurationSeconds < 0) throw new BusinessRuleException("INVALID_SERVICE_DURATION", "Service duration cannot be negative");
        if (distanceMeters < 0) throw new BusinessRuleException("INVALID_DISTANCE", "Distance cannot be negative");
    }
}
