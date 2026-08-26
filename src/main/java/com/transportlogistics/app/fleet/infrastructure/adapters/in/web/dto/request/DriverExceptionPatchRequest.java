package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import java.time.OffsetDateTime;

public record DriverExceptionPatchRequest(
        String exceptionType,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status,
        String reason,
        String remarks
) {
}
