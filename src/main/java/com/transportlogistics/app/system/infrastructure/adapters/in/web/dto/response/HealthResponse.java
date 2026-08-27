package com.transportlogistics.app.system.infrastructure.adapters.in.web.dto.response;

import java.time.OffsetDateTime;

public record HealthResponse(String status, OffsetDateTime timestamp) {
}
