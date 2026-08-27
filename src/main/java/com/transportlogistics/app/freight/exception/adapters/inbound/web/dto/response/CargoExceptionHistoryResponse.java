package com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CargoExceptionHistoryResponse(
        UUID id,
        String action,
        String actor,
        OffsetDateTime occurredAt,
        String reason,
        String details
) {}
