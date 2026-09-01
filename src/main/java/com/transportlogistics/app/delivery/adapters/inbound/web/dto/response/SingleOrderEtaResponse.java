package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.EtaSource;
import com.transportlogistics.app.delivery.domain.model.EtaStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SingleOrderEtaResponse(
        UUID orderId,
        OffsetDateTime estimatedArrivalAt,
        long travelDurationSeconds,
        long distanceMeters,
        EtaStatus slaStatus,
        EtaSource source,
        OffsetDateTime calculatedAt,
        OffsetDateTime staleAt,
        boolean isStale
) {}
