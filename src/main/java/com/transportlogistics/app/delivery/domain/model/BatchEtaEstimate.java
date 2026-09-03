package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BatchEtaEstimate(
        UUID batchId,
        OffsetDateTime calculatedAt,
        OffsetDateTime staleAt,
        long totalDurationSeconds,
        long totalDistanceMeters,
        OffsetDateTime estimatedCompletionAt,
        EtaSource source,
        List<BatchEtaStopEstimate> stops
) {
    public BatchEtaEstimate {
        if (batchId == null) throw new BusinessRuleException("BATCH_ID_REQUIRED", "Batch ID is required");
        if (calculatedAt == null) throw new BusinessRuleException("CALCULATED_AT_REQUIRED", "Calculated at timestamp is required");
        if (staleAt == null) throw new BusinessRuleException("STALE_AT_REQUIRED", "Stale at timestamp is required");
        if (totalDurationSeconds < 0) throw new BusinessRuleException("INVALID_DURATION", "Total duration cannot be negative");
        if (totalDistanceMeters < 0) throw new BusinessRuleException("INVALID_DISTANCE", "Total distance cannot be negative");
        if (estimatedCompletionAt == null) throw new BusinessRuleException("ESTIMATED_COMPLETION_REQUIRED", "Estimated completion time is required");
        if (source == null) throw new BusinessRuleException("SOURCE_REQUIRED", "ETA source is required");
        stops = stops == null ? List.of() : List.copyOf(stops);
    }

    public boolean isStale(OffsetDateTime now) {
        return now != null && now.isAfter(staleAt);
    }
}
