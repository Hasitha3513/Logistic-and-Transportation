package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverExceptionResponse(
        UUID id,
        UUID driverId,
        String exceptionType,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status,
        String reason,
        String remarks,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {
}
