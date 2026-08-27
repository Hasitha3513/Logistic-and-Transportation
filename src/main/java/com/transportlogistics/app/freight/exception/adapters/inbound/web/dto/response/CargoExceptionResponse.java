package com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CargoExceptionResponse(
        UUID id,
        String exceptionNumber,
        String exceptionType,
        String status,
        String severity,
        UUID freightOrderId,
        UUID manifestId,
        UUID manifestItemId,
        String description,
        String impact,
        String restriction,
        String correctiveAction,
        String resolution,
        OffsetDateTime resolvedAt,
        String resolvedBy,
        List<CargoExceptionHistoryResponse> history,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy,
        long version
) {}
