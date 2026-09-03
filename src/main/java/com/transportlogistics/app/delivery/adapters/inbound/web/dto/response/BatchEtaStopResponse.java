package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.EtaStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BatchEtaStopResponse(
        UUID deliveryOrderId,
        int sequence,
        OffsetDateTime estimatedArrivalAt,
        long travelDurationSeconds,
        long serviceDurationSeconds,
        long distanceMeters,
        EtaStatus slaStatus
) {}
