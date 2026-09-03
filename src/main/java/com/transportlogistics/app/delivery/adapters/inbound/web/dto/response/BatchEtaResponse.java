package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.EtaSource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BatchEtaResponse(
        UUID batchId,
        OffsetDateTime calculatedAt,
        OffsetDateTime staleAt,
        long totalDurationSeconds,
        long totalDistanceMeters,
        OffsetDateTime estimatedCompletionAt,
        EtaSource source,
        boolean isStale,
        List<BatchEtaStopResponse> stops
) {}
